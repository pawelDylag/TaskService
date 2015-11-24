
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Pawel Dylag on 15/11/15.
 * PRiR 2015/2016 - Jagiellonian University
 */
class SystemExec implements SystemInterface {

    /** All queue objects with their workers */
    private ArrayList<SystemQueue> mSystemQueues;

    /** Last id of task in system, that needs to keep order*/
    private int mLastAddedOrderedTaskId = -1;

    /** Most recent finished task id, that needs to keep order*/
    private int mRecentlyFinishedOrderedTaskId = -1;

    /** Most recent finished task lock*/
    private final ReadWriteLock mRecentlyFinishedOrderedTaskIdLock = new ReentrantReadWriteLock(true);

    private void writeRecentlyFinishedOrderedTaskId(int id) {
        mRecentlyFinishedOrderedTaskIdLock.writeLock().lock();
        try {
            this.mRecentlyFinishedOrderedTaskId = id;
        } finally {
            mRecentlyFinishedOrderedTaskIdLock.writeLock().unlock();
        }
    }

    private int readRecentlyFinishedOrderedTaskId() {
        int result;
        mRecentlyFinishedOrderedTaskIdLock.readLock().lock();
        try {
            result = mRecentlyFinishedOrderedTaskId;
        } finally {
            mRecentlyFinishedOrderedTaskIdLock.readLock().unlock();
        }
        return result;
    }

    private synchronized int updateLastAddedOrderedTaskId(int id) {
        int lastId = mLastAddedOrderedTaskId;
        this.mLastAddedOrderedTaskId = id;
        return lastId;
    }

    @Override
    public void setNumberOfQueues(int queues) {
        if(queues <= 0 ) {
            throw new IllegalArgumentException("Queues number must be positive");
        }
        mSystemQueues = new ArrayList<SystemQueue>();
        for (int i = 0; i < queues; i++) {
            mSystemQueues.add(new SystemQueue(i));
        }
    }

    @Override
    public void setThreadsLimit(int[] maximumThreadsPerQueue) {
        if (mSystemQueues == null) {
            throw new IllegalArgumentException("Queue number must be provided via setNumberOfQueues(int) before specifying threading policy.");
        }
        if (mSystemQueues.size() != maximumThreadsPerQueue.length) {
            throw new IllegalArgumentException("int[] maximumThreadsPerQueue must be of equal size as queues number.");
        }
        for (int i = 0; i < maximumThreadsPerQueue.length; i++) {
            mSystemQueues.get(i).startWorkers(maximumThreadsPerQueue[i]);
        }
    }

    @Override
    public void addTask(TaskInterface task) {
        new Thread(new DispatchNewTask(task)).run();
    }

    public String toString() {
        String result = "";
        for (SystemQueue q : mSystemQueues) {
            result += q.toString() + "\n";
        }
        return result;
    }

    /**
     * Moves task to next queue
     * @param fromQueueId - first queue
     * @param task - task to move
     */
    private void moveTaskToNextQueue(int fromQueueId, TaskWrapper task) {
        if(fromQueueId > mSystemQueues.size() - 1) {
            throw new IndexOutOfBoundsException("Queue index out of bounds");
        }
        if (task != null) {
            mSystemQueues.get(fromQueueId+1).addTask(task);
        }
    }

    /**
     * =============
     * PRIVATE CLASS
     * Used for dispatching new task in system.
     * It creates new thread which wraps taskInterface into TaskWrapper
     * and adds it to proper queue
     */
    private class DispatchNewTask implements Runnable {
        private TaskInterface mTaskInterface;

        public DispatchNewTask(TaskInterface mTaskInterface) {
            this.mTaskInterface = mTaskInterface;
        }

        @Override
        public void run() {
            if (mTaskInterface != null) {
                TaskWrapper task;
                if (mTaskInterface.keepOrder()) {
                    task = new TaskWrapper(mTaskInterface, updateLastAddedOrderedTaskId(mTaskInterface.getTaskID()));
                } else  task = new TaskWrapper(mTaskInterface,  -1);
                mSystemQueues.get(task.getFirstQueue()).addTask(task);
            }
        }

    }

    /**
     * =============
     * PRIVATE CLASS
     * This class represents single Queue in system,
     * with it's workers dynamically created via provided policy.
     */
    private class SystemQueue {

        private final TaskQueue mQueue;

        private ThreadPoolExecutor mThreadPoolExecutor;
        private final int mQueueId;

        public SystemQueue(int queueId) {
            this.mQueueId = queueId;
            mQueue = new TaskQueue(queueId);
        }

        public void startWorkers(int count) {
            mThreadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(count);
            for(int i = 0; i < count; i++) {
                mThreadPoolExecutor.execute(new Worker(i));
            }
        }

        public void addTask(TaskWrapper task) {
            synchronized (mQueue) {
                mQueue.add(task);
                mQueue.notifyAll();
            }
        }

        public String toString() {
            return mQueue.toString();
        }

        /**
         * =============
         * PRIVATE PRIVATE CLASS
         * This represents each thread work cycle.
         */
        private class Worker implements Runnable {
            private int threadId;

            public Worker(int threadId) {
                this.threadId = threadId;
            }

            @Override
            public void run() {
                while (true) {
                    TaskWrapper task = null;
                    synchronized(mQueue) {
                        boolean foundTask = false;
                        boolean foundUnreadyTask = false;
                        // check finishing tasks
                        if (!mQueue.getFinishingTaskQueue().isEmpty()) {
                            if (mQueue.getFinishingTaskQueue().peek().getPreviousTaskId() == readRecentlyFinishedOrderedTaskId()) {
                                foundTask = true;
                                task = mQueue.getFinishingTaskQueue().poll();
                            } else {
                                foundUnreadyTask = true;
                                task = null;
                            }
                        }
                        // check guest tasks
                        if (!foundTask && !mQueue.getGuestTaskQueue().isEmpty()) {
                            task = mQueue.getGuestTaskQueue().poll();
                            foundTask = true;
                        }
                        // check other tasks
                        if (!foundTask && !mQueue.getOtherTaskQueue().isEmpty()) {
                            task = mQueue.getOtherTaskQueue().pop();
                        }
                        // if every queue is empty -> sleep
                        if (task == null && !foundUnreadyTask) {
                            try {
                                mQueue.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (task != null) {
                        if (task.getLastQueue() == mQueueId) {
                            // finish task here
                            task.work(mQueueId);
                            if (task.keepOrder()) {
                                writeRecentlyFinishedOrderedTaskId(task.getTaskID());
                            }
                        } else {
                            moveTaskToNextQueue(mQueueId, task.work(mQueueId));
                        }
                    }
                }
            }

        }

        /**
         * =============
         * PRIVATE PRIVATE CLASS
         * Queue structure - it is divided into three buckets
         * 1. Finishing Task Queue -> for ordered tasks that will be finished in this queue
         * 2. Guest Task Queue -> for ordered tasks that have to be moved to next queues
         * 3. Other Task Stack -> for tasks that are asynchronous and not ordered
         */
        private class TaskQueue {
            private final PriorityQueue<TaskWrapper> mFinishingTaskQueue;
            private final PriorityQueue<TaskWrapper> mGuestTaskQueue;
            private final Stack<TaskWrapper> mOtherTaskQueue;
            private final int mQueueId;

            public TaskQueue(int id) {
                this.mFinishingTaskQueue = new PriorityQueue<TaskWrapper>();
                this.mGuestTaskQueue = new PriorityQueue<TaskWrapper>();
                this.mOtherTaskQueue = new Stack<TaskWrapper>();
                this.mQueueId = id;
            }

            public void add(TaskWrapper task) {
                if (task.keepOrder()){
                    if (task.getLastQueue() == this.mQueueId) {
                        mFinishingTaskQueue.add(task);
                    } else mGuestTaskQueue.add(task);
                } else mOtherTaskQueue.push(task);
            }

            public PriorityQueue<TaskWrapper> getFinishingTaskQueue() {
                return mFinishingTaskQueue;
            }

            public PriorityQueue<TaskWrapper> getGuestTaskQueue() {
                return mGuestTaskQueue;
            }

            public Stack<TaskWrapper> getOtherTaskQueue() {
                return mOtherTaskQueue;
            }

            public String toString() {
                String result = "";
                result += "Queue " + mQueueId + " -> ";
                result += "Finishing(";
                for (TaskWrapper t : mFinishingTaskQueue) {
                    result += t.getTaskID() + ",";
                }
                result += "), Guest(";
                for (TaskWrapper t : mGuestTaskQueue) {
                    result += t.getTaskID() + ",";
                }
                result += "), Other(";
                for (TaskWrapper t : mOtherTaskQueue) {
                    result += t.getTaskID() + ",";
                }
                result += ")";
                return result;
            }

        }

    }

    /**
     * =============
     * PRIVATE CLASS
     * Task wrapper provides comparable interface for tasks
     * It also provides previous ordered task id for ordered tasks.
     */
    final private class TaskWrapper implements Comparable {

        private final TaskInterface mTaskInterface;
        private final int mPreviousTaskId;

        public TaskWrapper(TaskInterface mTaskInterface, int mPreviousTaskId) {
            this.mTaskInterface = mTaskInterface;
            this.mPreviousTaskId = mPreviousTaskId;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof TaskWrapper) {
                return Integer.compare(this.getTaskID(), ((TaskWrapper) o).getTaskID());
            } else
                throw new ClassCastException("Invalid comparison type");
        }

        public int getFirstQueue() {
            return mTaskInterface.getFirstQueue();
        }

        public int getLastQueue() {
            return mTaskInterface.getLastQueue();
        }


        public int getTaskID() {
            return mTaskInterface.getTaskID();
        }


        public boolean keepOrder() {
            return mTaskInterface.keepOrder();
        }

        public int getPreviousTaskId() {
            return mPreviousTaskId;
        }

        public TaskWrapper work(int queue) {
            return new TaskWrapper(mTaskInterface.work(queue), this.mPreviousTaskId);
        }

    }



}
