#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <time.h>
#include <limits.h>
#include <float.h>

#define NUM_FLOORS 150
#define NUM_ELEV 8
#define SIM_TIME 3600
#define CAPACITY 20
#define OPEN_TIME 10

// Passenger structure, does not depend on others
typedef struct Passenger {
    int start;
    int end;
    int requestTime;
    int enterTime;
    int exitTime;
} Passenger;

// Forward declarations for others
typedef struct Elevator Elevator;
typedef struct Floor Floor;
typedef struct ElevatorSimulation ElevatorSimulation;

Passenger* newPassenger(int start, int end, int currTime) {
    Passenger* p = (Passenger*)malloc(sizeof(Passenger));
    p->start = start;
    p->end = end;
    p->requestTime = currTime;
    p->enterTime = 0;
    p->exitTime = 0;
    return p;
}

// Queue for passengers
typedef struct QueueItem {
    Passenger* passenger;
    struct QueueItem* next;
} QueueItem;

typedef struct Queue {
    QueueItem* front;
    QueueItem* rear;
    int count;
} Queue;

Queue* newQueue() {
    Queue* q = (Queue*)malloc(sizeof(Queue));
    q->front = NULL;
    q->rear = NULL;
    q->count = 0;
    return q;
}

void enqueue(Queue* q, Passenger* p) {
    QueueItem* node = (QueueItem*)malloc(sizeof(QueueItem));
    node->passenger = p;
    node->next = NULL;
    
    if (q->rear == NULL) {
        q->front = q->rear = node;
    } else {
        q->rear->next = node;
        q->rear = node;
    }
    q->count++;
}

Passenger* dequeue(Queue* q) {
    if (q->front == NULL) return NULL;
    
    QueueItem* temp = q->front;
    Passenger* p = temp->passenger;
    q->front = q->front->next;
    
    if (q->front == NULL) {
        q->rear = NULL;
    }
    
    free(temp);
    q->count--;
    return p;
}

Passenger* peekQueue(Queue* q) {
    if (q->front == NULL) return NULL;
    return q->front->passenger;
}

bool isQueueEmpty(Queue* q) {
    return q->front == NULL;
}

// Array for stops
typedef struct array {
    int* data;
    int size;
    int capacity;
} array;

array* newArray() {
    array* arr = (array*)malloc(sizeof(array));
    arr->capacity = 10;
    arr->size = 0;
    arr->data = (int*)malloc(arr->capacity * sizeof(int));
    return arr;
}

void addToArray(array* arr, int value) {
    // Check if already exists
    for (int i = 0; i < arr->size; i++) {
        if (arr->data[i] == value) return;
    }
    
    if (arr->size >= arr->capacity) {
        arr->capacity *= 2;
        arr->data = (int*)realloc(arr->data, arr->capacity * sizeof(int));
    }
    
    arr->data[arr->size++] = value;
    
    // Keep sorted
    for (int i = arr->size - 1; i > 0; i--) {
        if (arr->data[i] < arr->data[i-1]) {
            int temp = arr->data[i];
            arr->data[i] = arr->data[i-1];
            arr->data[i-1] = temp;
        } else {
            break;
        }
    }
}

bool containsInt(array* arr, int value) {
    for (int i = 0; i < arr->size; i++) {
        if (arr->data[i] == value) return true;
    }
    return false;
}

void removeInt(array* arr, int value) {
    for (int i = 0; i < arr->size; i++) {
        if (arr->data[i] == value) {
            for (int j = i; j < arr->size - 1; j++) {
                arr->data[j] = arr->data[j + 1];
            }
            arr->size--;
            return;
        }
    }
}

void freeArray(array* arr) {
    free(arr->data);
    free(arr);
}

struct Floor {
    int num;
    Queue* waiting;
};

Floor* newFloor(int num) {
    Floor* f = (Floor*)malloc(sizeof(Floor));
    f->num = num;
    f->waiting = newQueue();
    return f;
}

typedef enum {
    IDLE, UP, DOWN, OPEN
} ElevatorState;

// Passenger list for elevator
typedef struct PassengerNode {
    Passenger* passenger;
    struct PassengerNode* next;
} PassengerNode;

typedef struct PassengerList {
    PassengerNode* head;
    int count;
} PassengerList;

PassengerList* createPassengerList() {
    PassengerList* list = (PassengerList*)malloc(sizeof(PassengerList));
    list->head = NULL;
    list->count = 0;
    return list;
}

void addPassenger(PassengerList* list, Passenger* p) {
    PassengerNode* node = (PassengerNode*)malloc(sizeof(PassengerNode));
    node->passenger = p;
    node->next = list->head;
    list->head = node;
    list->count++;
}

void removePassengerFromList(PassengerList* list, Passenger* p) {
    PassengerNode* curr = list->head;
    PassengerNode* prev = NULL;
    
    while (curr != NULL) {
        if (curr->passenger == p) {
            if (prev == NULL) {
                list->head = curr->next;
            } else {
                prev->next = curr->next;
            }
            free(curr);
            list->count--;
            return;
        }
        prev = curr;
        curr = curr->next;
    }
}

struct Elevator {
    int num;
    int currFloor;
    int doorTimer;
    PassengerList* passengers;
    ElevatorState state;
    array* upStops;
    array* downStops;
    int lastPickupTime;
    ElevatorSimulation* sim;
};

struct ElevatorSimulation {
    long totalWaitTime;
    long totalTravelTime;
    long totalDistance;
    int numPassengers;
    Elevator* elevators[NUM_ELEV];
    Floor* floors[NUM_FLOORS];
};

ElevatorSimulation* newSim() {
    ElevatorSimulation* sim = (ElevatorSimulation*)malloc(sizeof(ElevatorSimulation));
    sim->totalWaitTime = 0;
    sim->totalTravelTime = 0;
    sim->totalDistance = 0;
    sim->numPassengers = 0;
    
    for (int i = 0; i < NUM_FLOORS; i++) {
        sim->floors[i] = newFloor(i);
    }
    
    return sim;
}

// Declarations for Elevator functions
void updateElevState(Elevator* e);
double calculateScore(Elevator* e, int targetFloor, bool goingUp, int currentTime);
void addRequest(Elevator* e, int floor, bool goingUp);
void updateElevator(Elevator* e, int time);
void moveUp(Elevator* e, int time);
void moveDown(Elevator* e, int time);
void handleDoors(Elevator* e, int time);
int unload(Elevator* e, int time);
void getNext(Elevator* e);
bool hasUp(Elevator* e);
bool hasDown(Elevator* e);
bool isTarget(Elevator* e, int floor);
int estimateDist(Elevator* e);

Elevator* newElevator(int id, ElevatorSimulation* sim) {
    Elevator* e = (Elevator*)malloc(sizeof(Elevator));
    e->num = id;
    e->sim = sim;
    e->state = IDLE;
    e->currFloor = 0;
    e->doorTimer = 0;
    e->passengers = createPassengerList();
    e->upStops = newArray();
    e->downStops = newArray();
    e->lastPickupTime = 0;
    return e;
}

bool canBoard(Elevator* e) {
    return e->passengers->count < CAPACITY;
}

void updateElevState(Elevator* e) {
    if (e->state == IDLE) {
        if (e->upStops->size > 0) {
            e->state = UP;
        } else if (e->downStops->size > 0) {
            e->state = DOWN;
        }
    }
}

void addRequest(Elevator* e, int floor, bool goingUp) {
    if (floor > e->currFloor) {
        addToArray(e->upStops, floor);
    } else if (floor < e->currFloor) {
        addToArray(e->downStops, floor);
    } else {
        if (e->state == IDLE) {
            e->state = OPEN;
            e->doorTimer = OPEN_TIME;
        }
    }
    updateElevState(e);
}

int estimateDist(Elevator* e) {
    int dist = 0;
    if (e->upStops->size > 0) {
        dist = abs(e->currFloor - e->upStops->data[e->upStops->size - 1]);
    }
    if (e->downStops->size > 0) {
        int downDist = abs(e->currFloor - e->downStops->data[0]);
        if (downDist > dist) dist = downDist;
    }
    return dist;
}

double calculateScore(Elevator* e, int targetFloor, bool goingUp, int currentTime) {
    int totalStops = e->upStops->size + e->downStops->size;
    
    if (e->passengers->count >= CAPACITY) {
        return DBL_MAX;
    }
    
    if (e->state == IDLE) {
        if (totalStops >= 8) return DBL_MAX;
        return abs(e->currFloor - targetFloor);
    }
    
    if (totalStops >= 5) {
        return DBL_MAX;
    }
    
    if (e->state == UP && goingUp && targetFloor >= e->currFloor) {
        return (targetFloor - e->currFloor) + (e->passengers->count * 2) + (totalStops * 10);
    }
    
    if (e->state == DOWN && !goingUp && targetFloor <= e->currFloor) {
        return (e->currFloor - targetFloor) + (e->passengers->count * 2) + (totalStops * 10);
    }
    
    if (e->state == UP && goingUp && targetFloor < e->currFloor) {
        int distToFinish = estimateDist(e);
        return distToFinish + (e->currFloor - targetFloor) + (e->passengers->count * 5);
    }
    
    if (e->state == DOWN && !goingUp && targetFloor > e->currFloor) {
        int distToFinish = estimateDist(e);
        return distToFinish + (targetFloor - e->currFloor) + (e->passengers->count * 5);
    }
    
    return DBL_MAX;
}

bool hasUp(Elevator* e) {
    PassengerNode* curr = e->passengers->head;
    while (curr != NULL) {
        if (curr->passenger->end > e->currFloor) {
            return true;
        }
        curr = curr->next;
    }
    return false;
}

bool hasDown(Elevator* e) {
    PassengerNode* curr = e->passengers->head;
    while (curr != NULL) {
        if (curr->passenger->end < e->currFloor) {
            return true;
        }
        curr = curr->next;
    }
    return false;
}

bool isTarget(Elevator* e, int floor) {
    PassengerNode* curr = e->passengers->head;
    while (curr != NULL) {
        if (curr->passenger->end == floor) {
            return true;
        }
        curr = curr->next;
    }
    return false;
}

void recordPassenger(ElevatorSimulation* sim, Passenger* p) {
    sim->totalWaitTime += (p->enterTime - p->requestTime);
    sim->totalTravelTime += (p->exitTime - p->requestTime);
    sim->numPassengers++;
}

void moveUp(Elevator* e, int time) {
    if (e->currFloor >= NUM_FLOORS - 1) {
        getNext(e);
        return;
    }
    
    e->currFloor++;
    e->sim->totalDistance++;
    
    bool shouldStop = false;
    
    PassengerNode* curr = e->passengers->head;
    while (curr != NULL) {
        if (curr->passenger->end == e->currFloor) {
            shouldStop = true;
            break;
        }
        curr = curr->next;
    }
    
    if (containsInt(e->upStops, e->currFloor)) {
        shouldStop = true;
        removeInt(e->upStops, e->currFloor);
    }
    
    if (shouldStop) {
        e->state = OPEN;
        e->doorTimer = OPEN_TIME;
    } else if (e->upStops->size == 0 && !hasUp(e)) {
        getNext(e);
    }
}

void moveDown(Elevator* e, int time) {
    if (e->currFloor <= 0) {
        getNext(e);
        return;
    }
    
    e->currFloor--;
    e->sim->totalDistance++;
    
    bool shouldStop = false;
    
    PassengerNode* curr = e->passengers->head;
    while (curr != NULL) {
        if (curr->passenger->end == e->currFloor) {
            shouldStop = true;
            break;
        }
        curr = curr->next;
    }
    
    if (containsInt(e->downStops, e->currFloor)) {
        shouldStop = true;
        removeInt(e->downStops, e->currFloor);
    }
    
    if (shouldStop) {
        e->state = OPEN;
        e->doorTimer = OPEN_TIME;
    } else if (e->downStops->size == 0 && !hasDown(e)) {
        getNext(e);
    }
}

int unload(Elevator* e, int time) {
    int count = 0;
    PassengerNode* curr = e->passengers->head;
    PassengerNode* prev = NULL;
    
    while (curr != NULL) {
        PassengerNode* next = curr->next;
        if (curr->passenger->end == e->currFloor) {
            curr->passenger->exitTime = time;
            recordPassenger(e->sim, curr->passenger);
            
            if (prev == NULL) {
                e->passengers->head = next;
            } else {
                prev->next = next;
            }
            
            free(curr->passenger);
            free(curr);
            e->passengers->count--;
            count++;
        } else {
            prev = curr;
        }
        curr = next;
    }
    
    return count;
}

void handleDoors(Elevator* e, int time) {
    unload(e, time);
    
    Floor* f = e->sim->floors[e->currFloor];
    
    if (f != NULL && canBoard(e)) {
        bool willGoUp = e->upStops->size > 0 || hasUp(e);
        bool willGoDown = e->downStops->size > 0 || hasDown(e);
        
        while (canBoard(e) && !isQueueEmpty(f->waiting)) {
            Passenger* p = peekQueue(f->waiting);
            if (p == NULL) break;
            
            bool passengerGoingUp = p->end > p->start;
            
            if ((willGoUp && passengerGoingUp) || 
                (willGoDown && !passengerGoingUp) ||
                (!willGoUp && !willGoDown)) {
                
                int newStop = p->end;
                bool wouldCreateNewStop = false;
                
                if (passengerGoingUp) {
                    wouldCreateNewStop = !containsInt(e->upStops, newStop) && 
                                        !isTarget(e, newStop);
                } else {
                    wouldCreateNewStop = !containsInt(e->downStops, newStop) && 
                                        !isTarget(e, newStop);
                }
                
                int totalStops = e->upStops->size + e->downStops->size;
                if (wouldCreateNewStop && totalStops >= 8 && e->passengers->count > 5) {
                    break;
                }
                
                p = dequeue(f->waiting);
                p->enterTime = time;
                addPassenger(e->passengers, p);
                e->lastPickupTime = time;
                
                if (passengerGoingUp) {
                    addToArray(e->upStops, p->end);
                } else {
                    addToArray(e->downStops, p->end);
                }
                
                willGoUp = e->upStops->size > 0 || hasUp(e);
                willGoDown = e->downStops->size > 0 || hasDown(e);
            } else {
                break;
            }
        }
    }
    
    e->doorTimer--;
    if (e->doorTimer <= 0) {
        getNext(e);
    }
}

void getNext(Elevator* e) {
    bool hasUpPassengers = hasUp(e);
    bool hasDownPassengers = hasDown(e);
    
    if (hasUpPassengers || e->upStops->size > 0) {
        e->state = UP;
    } else if (hasDownPassengers || e->downStops->size > 0) {
        e->state = DOWN;
    } else {
        e->state = IDLE;
    }
}

void updateElevator(Elevator* e, int time) {
    switch (e->state) {
        case UP:
            moveUp(e, time);
            break;
        case DOWN:
            moveDown(e, time);
            break;
        case OPEN:
            handleDoors(e, time);
            break;
        case IDLE:
            updateElevState(e);
            break;
    }
}

void processRequests(ElevatorSimulation* sim, int time) {
    Floor* upRequests[NUM_FLOORS];
    Floor* downRequests[NUM_FLOORS];
    int upCount = 0;
    int downCount = 0;
    
    for (int i = 0; i < NUM_FLOORS; i++) {
        Floor* f = sim->floors[i];
        if (isQueueEmpty(f->waiting)) continue;
        
        Passenger* p = peekQueue(f->waiting);
        if (p == NULL) continue;
        
        if (p->end > p->start) {
            upRequests[upCount++] = f;
        } else {
            downRequests[downCount++] = f;
        }
    }
    
    // Simple sort by waiting count (bubble sort)
    for (int i = 0; i < upCount - 1; i++) {
        for (int j = 0; j < upCount - i - 1; j++) {
            if (upRequests[j]->waiting->count < upRequests[j+1]->waiting->count) {
                Floor* temp = upRequests[j];
                upRequests[j] = upRequests[j+1];
                upRequests[j+1] = temp;
            }
        }
    }
    
    for (int i = 0; i < downCount - 1; i++) {
        for (int j = 0; j < downCount - i - 1; j++) {
            if (downRequests[j]->waiting->count < downRequests[j+1]->waiting->count) {
                Floor* temp = downRequests[j];
                downRequests[j] = downRequests[j+1];
                downRequests[j+1] = temp;
            }
        }
    }
    
    bool assignedElevators[NUM_ELEV] = {false};
    
    // Process up requests
    for (int i = 0; i < upCount; i++) {
        Floor* f = upRequests[i];
        if (isQueueEmpty(f->waiting)) continue;
        
        Elevator* best = NULL;
        double bestScore = DBL_MAX;
        
        for (int j = 0; j < NUM_ELEV; j++) {
            if (assignedElevators[j]) continue;
            
            double score = calculateScore(sim->elevators[j], f->num, true, time);
            
            if (score < DBL_MAX && score < bestScore) {
                bestScore = score;
                best = sim->elevators[j];
            }
        }
        
        if (best != NULL && bestScore < 500) {
            addRequest(best, f->num, true);
            assignedElevators[best->num] = true;
        }
    }
    
    // Process down requests
    for (int i = 0; i < downCount; i++) {
        Floor* f = downRequests[i];
        if (isQueueEmpty(f->waiting)) continue;
        
        Elevator* best = NULL;
        double bestScore = DBL_MAX;
        
        for (int j = 0; j < NUM_ELEV; j++) {
            if (assignedElevators[j]) continue;
            
            double score = calculateScore(sim->elevators[j], f->num, false, time);
            
            if (score < DBL_MAX && score < bestScore) {
                bestScore = score;
                best = sim->elevators[j];
            }
        }
        
        if (best != NULL && bestScore < 500) {
            addRequest(best, f->num, false);
            assignedElevators[best->num] = true;
        }
    }
}

void runSimulation(ElevatorSimulation* sim) {
    srand(time(NULL));
    
    for (int t = 0; t < SIM_TIME; t++) {
        // People entering building
        int numUp = rand() % 5;
        for (int i = 0; i < numUp; i++) {
            int dest = rand() % (NUM_FLOORS - 1) + 1;
            Passenger* p = newPassenger(0, dest, t);
            enqueue(sim->floors[0]->waiting, p);
        }
        
        // People leaving building
        int numDown = rand() % 5;
        for (int i = 0; i < numDown; i++) {
            int start = rand() % (NUM_FLOORS - 1) + 1;
            Passenger* p = newPassenger(start, 0, t);
            enqueue(sim->floors[start]->waiting, p);
        }
        
        for (int i = 0; i < NUM_ELEV; i++) {
            updateElevator(sim->elevators[i], t);
        }
        
        processRequests(sim, t);
    }
}

void printResults(ElevatorSimulation* sim) {
    /*printf("Total passengers delivered: %d\n", sim->numPassengers);
    
    int totalWaiting = 0;
    for (int i = 0; i < NUM_FLOORS; i++) {
        totalWaiting += sim->floors[i]->waiting->count;
    }
    printf("Passengers still waiting: %d\n", totalWaiting);
    
    printf("\nElevator Status:\n");
    for (int i = 0; i < NUM_ELEV; i++) {
        Elevator* e = sim->elevators[i];
        const char* stateStr[] = {"IDLE", "UP", "DOWN", "OPEN"};
        printf("Elevator %d: Floor %d, State: %s, Passengers: %d, Stops: %d\n",
               e->num, e->currFloor, stateStr[e->state], 
               e->passengers->count, e->upStops->size + e->downStops->size);
    }*/
    
    if (sim->numPassengers > 0) {
        printf("\nAvg wait time: %ld sec\n", sim->totalWaitTime / sim->numPassengers);
        printf("Avg total trip time: %ld sec\n", sim->totalTravelTime / sim->numPassengers);
    }
    printf("\nAvg distance per elevator: %ld floors\n", sim->totalDistance / NUM_ELEV);
}

int main() {
    ElevatorSimulation* sim = newSim();
    
    // Initialize elevators
    for (int i = 0; i < NUM_ELEV; i++) {
        sim->elevators[i] = newElevator(i, sim);
    }
    
    runSimulation(sim);
    printResults(sim);
    
    return 0;
}