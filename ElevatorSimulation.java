import java.util.*;

public class ElevatorSimulation {
    public static final int numFloors = 150;
    public static final int numElev = 8;
    public static final int simTime = 3600;
    public static final int capacity = 20;
    public static final int openTime = 10;

    public long totalWaitTime = 0;
    public long totalTravelTime = 0;
    public long totalDistance = 0;
    public int numPassengers = 0;

    private List<Elevator> elevators;
    private List<Floor> floors;

    public ElevatorSimulation() {
        elevators = new ArrayList<>();
        floors = new ArrayList<>();

        for (int i = 0; i < numElev; i++) {
            elevators.add(new Elevator(i, this));
        }
        for (int i = 0; i < numFloors; i++) {
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
        
        for (int time = 0; time < simTime; time++) {
            // People entering building
            int numUp = random.nextInt(5);
            for (int i = 0; i < numUp; i++) {
                int dest = random.nextInt(numFloors - 1) + 1;
                Passenger p = new Passenger(0, dest, time);
                floors.get(0).enqueue(p);
            }

            // People leaving building
            int numDown = random.nextInt(5);
            for (int i = 0; i < numDown; i++) {
                int start = random.nextInt(numFloors - 1) + 1;
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
        
        // Debug info
        int totalWaiting = 0;
        for (Floor f : floors) {
            totalWaiting += f.getWaitingCount();
        }
        System.out.println("Passengers still waiting: " + totalWaiting);
        
        // Elevator status
        System.out.println("\nElevator Status:");
        for (Elevator e : elevators) {
            e.printStatus();
        }
        
        if (numPassengers > 0) {
            System.out.println("\nAvg wait time: " + (totalWaitTime / numPassengers) + " sec");
            System.out.println("Avg total trip time: " + (totalTravelTime / numPassengers) + " sec");
        }
        System.out.println("\nAvg distance per elevator: " + (totalDistance / numElev) + " floors");
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
    private int num;
    private int currFloor;
    private int doorTimer;
    private List<Passenger> passengers;
    private State state;
    private ElevatorSimulation sim;
    
    // Using lists to track stops
    private List<Integer> upStops;
    private List<Integer> downStops;

    public enum State {
        IDLE, UP, DOWN, OPEN
    }

    public Elevator(int id, ElevatorSimulation sim) {
        this.num = id;
        this.sim = sim;
        this.state = State.IDLE;
        this.currFloor = 0;
        this.passengers = new ArrayList<>();
        this.upStops = new ArrayList<>();
        this.downStops = new ArrayList<>();
    }

    public boolean canBoard() {
        return passengers.size() < ElevatorSimulation.capacity;
    }

    public boolean canAcceptRequest() {
        // Limit queue to prevent one elevator from hogging all requests
        return state == State.IDLE || (upStops.size() + downStops.size() < 50);
    }

    public void addRequest(int floor, boolean goingUp) {
        // Add the floor based on what direction need to go
        
        if (floor > currFloor) {
            // Floor is above, add to upStops
            if (!upStops.contains(floor)) {
                upStops.add(floor);
            }
        } else if (floor < currFloor) {
            // Floor is below , add to downStops
            if (!downStops.contains(floor)) {
                downStops.add(floor);
            }
        } else {
            // At this floor, open doors
            if (state == State.IDLE) {
                state = State.OPEN;
                doorTimer = ElevatorSimulation.openTime;
            }
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

    // Heuristic function to choose best elevator
    public int calculateScore(int targetFloor, boolean goingUp) {
        if (state == State.IDLE) {
            return Math.abs(currFloor - targetFloor);
        } 
        // Going up and the request is above
        else if (state == State.UP && goingUp && targetFloor >= currFloor) {
            return targetFloor - currFloor;
        } 
        // Going down and the request is below
        else if (state == State.DOWN && !goingUp && targetFloor <= currFloor) {
            return currFloor - targetFloor;
        } 
        else {
            // Penalty if wrong direction
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
                updateState(); // Check if new tasks
                break;
        }
    }

    private void moveUp(int time) {
        if (currFloor >= ElevatorSimulation.numFloors - 1) {
            determineNextState();
            return;
        }
        
        currFloor++;
        sim.recordDistance();
        
        // Check if need to stop at this floor
        boolean shouldStop = false;
        
        for (Passenger p : passengers) {
            if (p.getEnd() == currFloor) {
                shouldStop = true;
                break;
            }
        }
        
        // Check if this floor has pickup request
        if (upStops.contains(currFloor)) {
            shouldStop = true;
            upStops.remove((Integer) currFloor);
        }
        
        if (shouldStop) {
            state = State.OPEN;
            doorTimer = ElevatorSimulation.openTime;
        } else if (upStops.isEmpty() && !hasPassengersGoingUp()) {
            // No more up destinations
            determineNextState();
        }
    }

    private void moveDown(int time) {
        if (currFloor <= 0) {
            determineNextState();
            return;
        }
        
        currFloor--;
        sim.recordDistance();
        
        // Check if need to stop at this floor
        boolean shouldStop = false;
        
        // Check if passenger needs to get off
        for (Passenger p : passengers) {
            if (p.getEnd() == currFloor) {
                shouldStop = true;
                break;
            }
        }
        
        // Check if this floor has pickup request
        if (downStops.contains(currFloor)) {
            shouldStop = true;
            downStops.remove((Integer) currFloor);
        }
        
        if (shouldStop) {
            state = State.OPEN;
            doorTimer = ElevatorSimulation.openTime;
        } else if (downStops.isEmpty() && !hasPassengersGoingDown()) {
            // No more down destinations
            determineNextState();
        }
    }
    
    private boolean hasPassengersGoingUp() {
        for (Passenger p : passengers) {
            if (p.getEnd() > currFloor) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasPassengersGoingDown() {
        for (Passenger p : passengers) {
            if (p.getEnd() < currFloor) {
                return true;
            }
        }
        return false;
    }

    private void handleDoors(int time) {
        // 1. Let people off
        unload(time);

        // 2. Let people on
        Floor f = sim.getFloor(currFloor);
        if (f != null) {
            // Determine next direction based on remaining stops
            boolean willGoUp = !upStops.isEmpty();
            boolean willGoDown = !downStops.isEmpty();
            
            while (canBoard() && f.hasWaiting()) {
                Passenger p = f.peek();
                boolean passengerGoingUp = p.getEnd() > p.getStart();
                
                // Board if: going our direction, or we have no direction yet
                if ((willGoUp && passengerGoingUp) || 
                    (willGoDown && !passengerGoingUp) ||
                    (!willGoUp && !willGoDown)) {
                    p = f.dequeue();
                    p.enter(time);
                    passengers.add(p);
                    
                    if (passengerGoingUp) {
                        if (!upStops.contains(p.getEnd())) {
                            upStops.add(p.getEnd());
                        }
                    } else {
                        if (!downStops.contains(p.getEnd())) {
                            downStops.add(p.getEnd());
                        }
                    }
                    
                    // Update direction flags as new passengers board
                    willGoUp = !upStops.isEmpty();
                    willGoDown = !downStops.isEmpty();
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
        // Clear any invalid stops
        upStops.removeIf(floor -> floor < 0 || floor >= ElevatorSimulation.numFloors);
        downStops.removeIf(floor -> floor < 0 || floor >= ElevatorSimulation.numFloors);
        
        if (!upStops.isEmpty()) {
            state = State.UP;
        } else if (!downStops.isEmpty()) {
            state = State.DOWN;
        } else {
            state = State.IDLE;
        }
    }
    
    public void printStatus() {
        System.out.println("Elevator " + num + ": Floor " + currFloor + ", State: " + state + 
                         ", Passengers: " + passengers.size());
        System.out.println("  UpStops: " + upStops);
        System.out.println("  DownStops: " + downStops);
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
    public int getWaitingCount() { return waiting.size(); }
    public void enqueue(Passenger p) { waiting.add(p); }
    public Passenger dequeue() { return waiting.poll(); }
    public Passenger peek() { return waiting.peek(); }
}