import java.util.*;

public class ElevatorSimulation{
    public static final int numFloors = 150;
    public static final int capacity = 20;
    public static final int numElevators = 8;
    public static final int openTime = 10;
    public static final int simTime = 3600;
    public int totalWait;
    public int totalDist;
    public int numPassengers;
    public List<Passenger> allPassengers;
    private List<Elevator> elevators;
    private List<Floor> floors;
    public ElevatorSimulation(){
        allPassengers = new ArrayList<Passenger>();
        elevators = new ArrayList<Elevator>();
        floors = new ArrayList<Floor>();
        totalWait=0;
        totalDist=0;
        numPassengers=0;
        for(int i=0; i<8; i++){
            Elevator elevator = new Elevator(i+1);
        }
        for(int i=0; i<150; i++){
            Floor floor = new Floor(i+1);
        }
    }
    private void runSim(){

    }
    private void printData(){
        System.out.println("The average wait time was " + totalWait/numPassengers);
        System.out.println("The average distance was " + totalDist/numPassengers);
        StringBuilder eachTime = new StringBuilder();

    }
    public void processRequests(int currTime){

    }
    public void updateElevators(int currTime){

    }
    private Elevator findBestOption(int floor, boolean goingUp){
        
    }
}
class Passenger{
    private int start;
    private int end;
    private int requestTime;
    private int enterTime;
    private int exitTime;
    public Passenger(int start, int end, int currTime, ElevatorSimulation sim){
        this.start=start;
        this.end=end;
        this.requestTime=currTime;

    }
    public int getWait(){
        return enterTime-requestTime;
    }
    public int getTotal(){
        return exitTime-requestTime;
    }

}
class Elevator{
    private int num;
    private int currFloor;
    private int destFloor;
    private int timer;
    private int totalFloors;
    private Set<Passenger> passengers;
    private int numPassengers;
    private State currState;

    public enum State{IDLE, UP, DOWN, OPEN}

    public Elevator(int num){
        this.num=num;
        currState=IDLE;
        currFloor=0;
    }

    public void move(){

    }
    public void open(){

    }
    public boolean canBoard(){

    }
    public boolean addPassenger(Passenger p){
        if(!canBoard){
            return false;
        }
        passengers.add(p);
        numPassengers++;
        return true;
    }
    public void unload(){

    }

}
class Floor{
    private int num;
    private Queue<Passenger> waiting;
    public Floor(int num){
        this.num=num;
        waiting = new Queue();
    }
    public void addPassenger(Passenger p){

    }
    public boolean hasWaiting(){

    }

}