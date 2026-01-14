import java.util.*;

public class ElevatorSimulation {
    public static final int NUM_FLOORS = 150;
    public static final int NUM_ELEVATORS = 8;
    public static final int TIME = 3600;
    public static final int CAPACITY = 20;
    public static final int OPEN_TIME = 10;

    public long totalWaitTime = 0;
    public long totalTravelTime = 0;
    public long totalDistance = 0;
    public int numPassengers = 0;

    private List<Elevator> elevators;
    private List<Floor> floors;

    public ElevatorSimulation() {
        elevators = new ArrayList<>();
        floors = new ArrayList<>();

        for (int i = 0; i < NUM_ELEVATORS; i++) {
            elevators.add(new Elevator(i, this));
        }
        for (int i = 0; i < NUM_FLOORS; i++) {
            floors.add(new Floor(i));
        }
    }

    public Floor getFloor(int index) {
        if (index >= 0 && index < floors.size()) {
            return floors.get(index);
        }
        return null;
    }

    private void runSim() {
        Random random = new Random();
        
        for (int time = 0; time < TIME; time++) {
            // people entering building
            int numUp = random.nextInt(5);
            for (int i = 0; i < numUp; i++) {
                int dest = random.nextInt(NUM_FLOORS - 1) + 1;
                Passenger p = new Passenger(0, dest, time);
                floors.get(0).enqueue(p);
            }

            // people exiting building
            int numDown = random.nextInt(5);
            for (int i = 0; i < numDown; i++) {
                int start = random.nextInt(NUM_FLOORS - 1) + 1;
                Passenger p = new Passenger(start, 0, time);
                floors.get(start).enqueue(p);
            }
            
            processRequests();
            updateElevators(time);
        }
        printData();
    }

    private void processRequests() {
        for (Floor f : floors) {
            if (f.hasWaiting()) {
                Passenger p = f.peek();
                // Determine direction based on the first person in line
                boolean goingUp = p.getEnd() > p.getStart();
                
                Elevator bestElevator = findBestElevator(f.getNum(), goingUp);
                
                if (bestElevator != null && bestElevator.canAcceptRequest()) {
                    bestElevator.addRequest(f.getNum(), goingUp);
                }
            }
        }
    }

    private Elevator findBestElevator(int floor, boolean goingUp) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            int score = e.calculateScore(floor, goingUp);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    public void updateElevators(int currTime) {
        for (Elevator e : elevators) {
            e.update(currTime);
        }
    }

    public void recordPassenger(Passenger p) {
        totalWaitTime += p.getWait();
        totalTravelTime += p.getTotalTime();
        numPassengers++;
    }
    
    public void recordDistance() {
        totalDistance++;
    }

    private void printData() {
        System.out.println("--- Simulation Results ---");
        System.out.println("Total passengers delivered: " + numPassengers);
        if (numPassengers > 0) {
            System.out.println("Avg wait time: " + (totalWaitTime / numPassengers) + " sec");
            System.out.println("Avg total trip time: " + (totalTravelTime / numPassengers) + " sec");
        }
        System.out.println("Avg distance per elevator: " + (totalDistance / NUM_ELEVATORS) + " floors");
    }

    public static void main(String[] args) {
        new ElevatorSimulation().runSim();
    }
}

class Passenger {
    private int start;
    private int end;
    private int requestTime;
    private int enterTime;
    private int exitTime;

    public Passenger(int start, int end, int currTime) {
        this.start = start;
        this.end = end;
        this.requestTime = currTime;
    }

    public int getStart() { return start; }
    public int getEnd() { return end; }

    public void enter(int time) { enterTime = time; }
    public void exit(int time) { exitTime = time; }

    public int getWait() { return enterTime - requestTime; }
    public int getTotalTime() { return exitTime - requestTime; }
}

class Elevator {
    private int id;
    private int currFloor;
    private int doorTimer;
    private List<Passenger> passengers;
    private State state;
    private ElevatorSimulation sim;
    
    // Using TreeSets to keep stops sorted
    private TreeSet<Integer> upStops;
    private TreeSet<Integer> downStops;

    public enum State {
        IDLE, UP, DOWN, OPEN
    }

    public Elevator(int id, ElevatorSimulation sim) {
        this.id = id;
        this.sim = sim;
        this.state = State.IDLE;
        this.currFloor = 0;
        this.passengers = new ArrayList<>();
        this.upStops = new TreeSet<>();
        this.downStops = new TreeSet<>();
    }

    public boolean canBoard() {
        return passengers.size() < ElevatorSimulation.CAPACITY;
    }

    public boolean canAcceptRequest() {
        // Limit queue size to prevent one elevator from hogging all requests
        return state == State.IDLE || (upStops.size() + downStops.size() < 20);
    }

    public void addRequest(int floor, boolean goingUp) {
        if (goingUp) {
            upStops.add(floor);
        } else {
            downStops.add(floor);
        }
        updateState();
    }

    private void updateState() {
        if (state == State.IDLE) {
            if (!upStops.isEmpty()) {
                state = State.UP;
            } else if (!downStops.isEmpty()) {
                state = State.DOWN;
            }
        }
    }

    // Heuristic function to determine suitability for a request
    public int calculateScore(int targetFloor, boolean goingUp) {
        if (state == State.IDLE) {
            return Math.abs(currFloor - targetFloor);
        } 
        // If we are going up and the request is above us
        else if (state == State.UP && goingUp && targetFloor >= currFloor) {
            return targetFloor - currFloor;
        } 
        // If we are going down and the request is below us
        else if (state == State.DOWN && !goingUp && targetFloor <= currFloor) {
            return currFloor - targetFloor;
        } 
        else {
            // Heavy penalty if wrong direction
            return 1000 + Math.abs(currFloor - targetFloor);
        }
    }

    public void update(int time) {
        switch (state) {
            case UP:
                moveUp(time);
                break;
            case DOWN:
                moveDown(time);
                break;
            case OPEN:
                handleDoors(time);
                break;
            case IDLE:
                updateState(); // Check if we have new tasks
                break;
        }
    }

    private void moveUp(int time) {
        currFloor++;
        sim.recordDistance();
        
        if (upStops.contains(currFloor)) {
            state = State.OPEN;
            doorTimer = ElevatorSimulation.OPEN_TIME;
            upStops.remove(currFloor);
        }
    }

    private void moveDown(int time) {
        currFloor--;
        sim.recordDistance();
        
        if (downStops.contains(currFloor)) {
            state = State.OPEN;
            doorTimer = ElevatorSimulation.OPEN_TIME;
            downStops.remove(currFloor);
        }
    }

    private void handleDoors(int time) {
        // 1. Let people off
        unload(time);

        // 2. Let people on
        Floor f = sim.getFloor(currFloor);
        if (f != null) {
            // Determine logical direction for boarding
            boolean currentlyGoingUp = (state == State.UP || (!upStops.isEmpty() && downStops.isEmpty()));
            
            while (canBoard() && f.hasWaiting()) {
                Passenger p = f.peek();
                boolean passengerGoingUp = p.getEnd() > p.getStart();
                
                // Only pick up passengers going our way, unless we are IDLE
                if (state == State.IDLE || passengerGoingUp == currentlyGoingUp) {
                    p = f.dequeue();
                    p.enter(time);
                    passengers.add(p);
                    
                    if (passengerGoingUp) upStops.add(p.getEnd());
                    else downStops.add(p.getEnd());
                } else {
                    break; // Next passenger wants to go the other way
                }
            }
        }

        // 3. Close doors check
        doorTimer--;
        if (doorTimer <= 0) {
            determineNextState();
        }
    }

    private void unload(int time) {
        Iterator<Passenger> it = passengers.iterator();
        while (it.hasNext()) {
            Passenger p = it.next();
            if (p.getEnd() == currFloor) {
                p.exit(time);
                sim.recordPassenger(p);
                it.remove();
            }
        }
    }

    private void determineNextState() {
        if (!upStops.isEmpty()) {
            state = State.UP;
        } else if (!downStops.isEmpty()) {
            state = State.DOWN;
        } else {
            state = State.IDLE;
        }
    }
}

class Floor {
    private int num;
    private Queue<Passenger> waiting;

    public Floor(int num) {
        this.num = num;
        waiting = new LinkedList<>();
    }

    public int getNum() { return num; }
    public boolean hasWaiting() { return !waiting.isEmpty(); }
    public void enqueue(Passenger p) { waiting.add(p); }
    public Passenger dequeue() { return waiting.poll(); }
    public Passenger peek() { return waiting.peek(); }
}