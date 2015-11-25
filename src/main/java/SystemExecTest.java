import java.util.Arrays;
import java.util.Random;

/**
 * Created by paweldylag on 22/11/15.
 */
public class SystemExecTest {


    public static void main(String[] args) {
        System.out.println("SystemExecTest");
        test(new int[]{8, 8, 8, 8, 8},20);
    }

    private static void test(int[] policy, int taskCount) {
        int queueCount = policy.length;
        System.out.println("STARTING TEST FOR " + queueCount + " QUEUES WITH POLICY " + Arrays.toString(policy));
        Random r = new Random();
        int index = 0;
        try {
            SystemExec system = new SystemExec();
            system.setNumberOfQueues(queueCount);
            system.setThreadsLimit(policy);
            for (int i = 0; i < taskCount; i++) {
                int firstQueue = r.nextInt(queueCount);
                int lastQueue = firstQueue + r.nextInt(queueCount - firstQueue);
                boolean keepOrder = r.nextBoolean();
                long workTime = (long) r.nextInt(1000);
                TestTask task = new TestTask(firstQueue, lastQueue, index++, keepOrder, workTime);
                system.addTask(task);
            }
            Thread.sleep(10000);
            System.out.println(system.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}
