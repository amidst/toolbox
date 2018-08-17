package eu.amidst.kbspaper;

/**
 * Created by rcabanas on 14/08/2018.
 */
public class Monitor extends Thread{

    public long max_mem = 0;


    public void run(){


        try {

            while (true) {

                Runtime runtime = Runtime.getRuntime();

                long mem = runtime.totalMemory();

                //System.out.println("memory used: " + used);

                if (max_mem < mem)
                    max_mem = mem;

                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



    public static void main(String[] args) throws InterruptedException {

        Monitor m = new Monitor();
        m.start();

        Thread.sleep(1000);

        m.stop();
        System.out.println("Peak memory:"+m.max_mem);




    }


}
