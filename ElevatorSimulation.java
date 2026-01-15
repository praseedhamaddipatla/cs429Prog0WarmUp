import java.util.*;

public class ElevatorSimulation {
    public static final int numFloors = 150;
    public static final int numElev = 8;
    public static final int simTime = 3600;
    public static final int capacity = 20;
    public static final int openTime = 10;

    public long totalWait = 0;
    public long totalTravel = 0;
    public long totalDist = 0;
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
            
            updateElevators(time);
            processRequests(time);
        }
        printData();
    }

    private void processRequests(int time) {        
        // Separate up and down requests
        List<Floor> upRequests = new ArrayList<>();
        List<Floor> downRequests = new ArrayList<>();
        
        for (Floor f : floors) {
            if (!f.hasWaiting()) continue;
            
            Passenger p = f.peek();
            if (p == null) continue;
            
            if (p.getEnd() > p.getStart()) {
                upRequests.add(f);
            } else {
                downRequests.add(f);
            }
        }
        
        // Sort by number of waiting passengers
        upRequests.sort((a, b) -> Integer.compare(b.getWaitingCount(), a.getWaitingCount()));
        downRequests.sort((a, b) -> Integer.compare(b.getWaitingCount(), a.getWaitingCount()));
        
        assignElevatorsToRequests(upRequests, true, time);
        assignElevatorsToRequests(downRequests, false, time);
    }
    
    private void assignElevatorsToRequests(List<Floor> requests, boolean goingUp, int time) {
        Set<Integer> assignedElevators = new HashSet<>();
        
        for (Floor f : requests) {
            if (!f.hasWaiting()) continue;
            
            // Find best elevator
            Elevator best = null;
            double bestScore = Double.MAX_VALUE;
            
            for (Elevator e : elevators) {
                if (assignedElevators.contains(e.num)) continue;
                
                double score = e.calculateScore(f.getNum(), goingUp, time);
                
                // Only consider reasonable elevators
                if (score < Double.MAX_VALUE && score < bestScore) {
                    bestScore = score;
                    best = e;
                }
            }
            
            if (best != null && bestScore < 500) {
                best.addRequest(f.getNum(), goingUp);
                assignedElevators.add(best.num);
            }
        }
    }

    public void updateElevators(int currTime) {
        for (Elevator e : elevators) {
            e.update(currTime);
        }
    }

    public void recordPassenger(Passenger p) {
        totalWait += p.getWait();
        totalTravel += p.getTotalTime();
        numPassengers++;
    }
    
    public void recordDistance() {
        totalDist++;
    }

    private void printData(){
        System.out.println("Total passengers delivered: " + numPassengers);
        if (numPassengers > 0) {
            System.out.println("Avg wait time: " + (totalWait / numPassengers) + " sec");
            System.out.println("Avg total trip time: " + (totalTravel / numPassengers) + " sec");
        }
        System.out.println("Avg distance per elevator: " + (totalDist / numElev) + " floors");
    }

    private void printDataDebug() {
        System.out.println("Total passengers delivered: " + numPassengers);
        
        int totalWaiting = 0;
        for (Floor f : floors) {
            totalWaiting += f.getWaitingCount();
        }
        System.out.println("Passengers still waiting: " + totalWaiting);
        
        System.out.println("\nElevator Status:");
        for (Elevator e : elevators) {
            e.printStatus();
        }
        
        if (numPassengers > 0) {
            System.out.println("\nAvg wait time: " + (totalWait / numPassengers) + " sec");
            System.out.println("Avg total trip time: " + (totalTravel / numPassengers) + " sec");
        }
        System.out.println("\nAvg distance per elevator: " + (totalDist / numElev) + " floors");
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
    public int getRequestTime() { return requestTime; }

    public void enter(int time) { enterTime = time; }
    public void exit(int time) { exitTime = time; }

    public int getWait() { return enterTime - requestTime; }
    public int getTotalTime() { return exitTime - requestTime; }
}

class Elevator {
    public int num;
    private int currFloor;
    private int doorTimer;
    private List<Passenger> passengers;
    private State state;
    private ElevatorSimulation sim;
    
    private TreeSet<Integer> upStops;
    private TreeSet<Integer> downStops;

    public enum State {
        IDLE, UP, DOWN, OPEN
    }

    public Elevator(int name, ElevatorSimulation sim) {
        this.num = name;
        this.sim = sim;
        this.state = State.IDLE;
        this.currFloor = 0;
        this.passengers = new ArrayList<>();
        this.upStops = new TreeSet<>();
        this.downStops = new TreeSet<>();
    }

    public boolean canBoard() {
        return passengers.size() < ElevatorSimulation.capacity;
    }

    public void addRequest(int floor, boolean goingUp) {
        if (floor > currFloor) {
            upStops.add(floor);
        } else if (floor < currFloor) {
            downStops.add(floor);
        } else {
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

    public double calculateScore(int targetFloor, boolean goingUp, int currentTime) {
        // Don't accept if overloaded
        int totalStops = upStops.size() + downStops.size();
        
        if (passengers.size() >= ElevatorSimulation.capacity) {
            return Double.MAX_VALUE;
        }
        
        if (state == State.IDLE) {
            // Idle elevators are best
            if (totalStops >= 8) return Double.MAX_VALUE;
            return Math.abs(currFloor - targetFloor);
        }
        
        if (totalStops >= 15) {
            return Double.MAX_VALUE; // Too busy
        }
        
        // Same direction and target otw
        if (state == State.UP && goingUp && targetFloor >= currFloor) {
            return (targetFloor - currFloor) + (passengers.size() * 2) + (totalStops * 10);
        }
        
        if (state == State.DOWN && !goingUp && targetFloor <= currFloor) {
            return (currFloor - targetFloor) + (passengers.size() * 2) + (totalStops * 10);
        }
        
        // Same direction but passed
        if (state == State.UP && goingUp && targetFloor < currFloor) {
            return Double.MAX_VALUE;
        }
        
        if (state == State.DOWN && !goingUp && targetFloor > currFloor) {
            return Double.MAX_VALUE;
        }
        
        // Wrong direction entirely
        return Double.MAX_VALUE;
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
                updateState();
                break;
        }
    }

    private void moveUp(int time) {
        if (currFloor >= ElevatorSimulation.numFloors - 1) {
            getNext();
            return;
        }
        
        currFloor++;
        sim.recordDistance();
        
        boolean shouldStop = false;
        
        for (Passenger p : passengers) {
            if (p.getEnd() == currFloor) {
                shouldStop = true;
                break;
            }
        }
        
        if (upStops.contains(currFloor)) {
            shouldStop = true;
            upStops.remove(currFloor);
        }
        
        if (shouldStop) {
            state = State.OPEN;
            doorTimer = ElevatorSimulation.openTime;
        } else if (upStops.isEmpty() && !hasUp()) {
            getNext();
        }
    }

    private void moveDown(int time) {
        if (currFloor <= 0) {
            getNext();
            return;
        }
        
        currFloor--;
        sim.recordDistance();
        
        boolean shouldStop = false;
        
        for (Passenger p : passengers) {
            if (p.getEnd() == currFloor) {
                shouldStop = true;
                break;
            }
        }
        
        if (downStops.contains(currFloor)) {
            shouldStop = true;
            downStops.remove(currFloor);
        }
        
        if (shouldStop) {
            state = State.OPEN;
            doorTimer = ElevatorSimulation.openTime;
        } else if (downStops.isEmpty() && !hasDown()) {
            getNext();
        }
    }
    
    private boolean hasUp() {
        for (Passenger p : passengers) {
            if (p.getEnd() > currFloor) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasDown() {
        for (Passenger p : passengers) {
            if (p.getEnd() < currFloor) {
                return true;
            }
        }
        return false;
    }

    private void handleDoors(int time) {
        unload(time);
        
        Floor f = sim.getFloor(currFloor);
        
        if (f != null && canBoard()) {
            boolean willGoUp = !upStops.isEmpty() || hasUp();
            boolean willGoDown = !downStops.isEmpty() || hasDown();
            
            // Try to fill
            while (canBoard() && f.hasWaiting()) {
                Passenger p = f.peek();
                if (p == null) break;
                
                boolean passengerGoingUp = p.getEnd() > p.getStart();
                
                // Board if direction matches or if idle
                if ((willGoUp && passengerGoingUp) || 
                    (willGoDown && !passengerGoingUp) ||
                    (!willGoUp && !willGoDown)) {
                    
                    // Check if too many diff stops
                    int newStop = p.getEnd();
                    boolean wouldCreateNewStop = false;
                    
                    if (passengerGoingUp) {
                        wouldCreateNewStop = !upStops.contains(newStop) && 
                                            !isTarget(newStop);
                    } else {
                        wouldCreateNewStop = !downStops.contains(newStop) && 
                                            !isTarget(newStop);
                    }
                    
                    // Limit new stops when elevator is busy
                    int totalStops = upStops.size() + downStops.size();
                    if (wouldCreateNewStop && totalStops >= 8 && passengers.size() > 5) {
                        break;
                    }
                    
                    p = f.dequeue();
                    p.enter(time);
                    passengers.add(p);
                    
                    if (passengerGoingUp) {
                        upStops.add(p.getEnd());
                    } else {
                        downStops.add(p.getEnd());
                    }
                    
                    willGoUp = !upStops.isEmpty() || hasUp();
                    willGoDown = !downStops.isEmpty() || hasDown();
                } else {
                    break;
                }
            }
        }

        doorTimer--;
        if (doorTimer <= 0) {
            getNext();
        }
    }
    
    private boolean isTarget(int floor) {
        for (Passenger p : passengers) {
            if (p.getEnd() == floor) return true;
        }
        return false;
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

    private void getNext() {
        upStops.removeIf(floor -> floor < 0 || floor >= ElevatorSimulation.numFloors);
        downStops.removeIf(floor -> floor < 0 || floor >= ElevatorSimulation.numFloors);
        
        boolean hasUpPassengers = hasUp();
        boolean hasDownPassengers = hasDown();
        
        // Prioritize current passengers
        if (hasUpPassengers || !upStops.isEmpty()) {
            state = State.UP;
        } else if (hasDownPassengers || !downStops.isEmpty()) {
            state = State.DOWN;
        } else {
            state = State.IDLE;
        }
    }
    
    public void printStatus() {
        System.out.println("Elevator " + num + ": Floor " + currFloor + ", State: " + state + 
                         ", Passengers: " + passengers.size() +
                         ", Stops: " + (upStops.size() + downStops.size()));
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