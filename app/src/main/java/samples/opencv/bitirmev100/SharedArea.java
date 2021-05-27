package samples.opencv.bitirmev100;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class SharedArea {

    private ArrayBlockingQueue<Integer> data;
    private int  readIndex = 0;


    SharedArea() {
        data = new ArrayBlockingQueue<>(500);
    }


    public void set( int value) throws InterruptedException {
        data.put(value); //value buffer a eklendi.
        System.out.println("Eleman yazildi : " + value + "\n Buffer ' ın hucre indexi" + data.size());
        System.out.println("--------------------------------");
        return;
    }

    public int get() throws  InterruptedException {
        int readValue = data.take(); //remove value from buffer


        return readValue;
    }

    public int filledSize() {
        return this.data.size();
    }







}
