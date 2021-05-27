package samples.opencv.bitirmev100;

public class ReadData {
    SharedArea sharedArea = new SharedArea();

    ReadData(SharedArea sharedArea) {
        this.sharedArea = sharedArea;
    }

    int getData() throws InterruptedException {
        int tmp;
        try {
            tmp = sharedArea.get();
            System.out.println("Veri okundu : " + tmp);
            return tmp;

        }catch (Exception e) {
            return -1; //error
        }
    }




    }


