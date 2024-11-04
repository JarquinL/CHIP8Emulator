package EMULATOR;

import CHIP.Chip;

public class Main extends Thread{
    private Chip chip8;
    private ChipFrame frame;

    public Main() {
        this.chip8 = new Chip();
        this.chip8.loadProgram("./pong2.c8");
        this.frame = new ChipFrame(chip8);
    }

    public void run(){
        //60 HRTZ
        while(true){
            this.chip8.run();
            if(this.chip8.needsRedraw()){
                this.frame.repaint();
                this.chip8.removeDrawFlag();
            }
            try{
                Thread.sleep(16);
            }
            catch(InterruptedException e){

            }
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }
}
