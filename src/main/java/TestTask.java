class TestTask  implements TaskInterface{
    private int firstQueue;
    private int lastQueue;
    private int id;
    private boolean keepOrder;
    private long workTime;
    private String stackTrace = " ";

    public TestTask(int firstQueue, int lastQueue, int id, boolean keepOrder, long workTime) {
        this.firstQueue = firstQueue;
        this.lastQueue = lastQueue;
        this.id = id;
        this.keepOrder = keepOrder;
        this.workTime = workTime;
    }

    @Override
    public int getFirstQueue() {
        return firstQueue;
    }

    @Override
    public int getLastQueue() {
        return lastQueue;
    }

    @Override
    public int getTaskID() {
        return id;
    }

    @Override
    public boolean keepOrder() {
        return keepOrder;
    }

    @Override
    public TaskInterface work(int queue) {
        try {
            Thread.sleep(workTime);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        if (this.firstQueue == this.lastQueue) {
            stackTrace += queue + "";
            System.out.println(" ===> FINISHED TASK " + getTaskID() + isOrdered() + " , workTime = " + workTime + ", trace: " + stackTrace);
        } else {
            stackTrace += queue + " -> ";
            //System.out.println(" =========> MOVING TASK " + getTaskID() + isOrdered() + " , workTime = " + workTime + ", trace: " + stackTrace);
            this.firstQueue++;
        }
        return this;
    }

    private String isOrdered() {
        if (this.keepOrder){
            return "!";
        } else return "";
    }

}