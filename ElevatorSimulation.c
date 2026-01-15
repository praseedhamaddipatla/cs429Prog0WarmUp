#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <time.h>
#include <limits.h>
#include <float.h>

#define numFloors 150
#define numElev 8
#define simTime 3600
#define capacity 20
#define openTime 1

typedef struct Passenger{
    int start;
    int end;
    int requestTime;
    int enterTime;
    int exitTime;

    struct Passenger* next;
} Passenger;

typedef struct Floor{
    int num;
    Passenger* head;
    Passenger* tail;
    int waitingCount;
} Floor;

typedef enum State {IDLE, UP, DOWN, OPEN} State;

typedef struct Elevator{
    int num;
    int currFloor;
    int doorTimer;

    Passenger* passengers[capacity];
    int passengerCount;
    State state;

    int upStops[numFloors];
    int upCount;
    int downStops[numFloors];
    int downCount;
} Elevator;

long totalWait=0;
long totalTravel=0;
long totalDist=0;
int numPassengers=0;

Floor floors[numFloors];
Elevator elevators[numElev];

Passenger* newPassenger(int start, int end, int time){
    Passenger* p = malloc(sizeof(Passenger));

    p->start=start;
    p->end=end;
    p->requestTime=time;
    p->enterTime=0;
    p->exitTime=0;
    p->next=NULL;
    return p;
}

int getWait(Passenger* p) {
    return p->enterTime - p->requestTime;
}

int getTotalTime(Passenger* p) {
    return p->exitTime - p->requestTime;
}

void enqueue(Floor* f, Passenger* p) {

    if (f->tail == NULL) {
        f->head = f->tail = p;
    } else {
        f->tail->next = p;
        f->tail = p;
    }

    f->waitingCount++;
}

Passenger* dequeue(Floor* f) {

    if (f->head == NULL)
        return NULL;

    Passenger* p = f->head;

    f->head = p->next;

    if (f->head == NULL)
        f->tail = NULL;

    p->next = NULL;

    f->waitingCount--;

    return p;
}

Passenger* peek(Floor* f) {
    return f->head;
}

bool hasWaiting(Floor* f) {
    return f->waitingCount > 0;
}

bool contains(int* arr, int count, int value) {

    for (int i = 0; i < count; i++)
        if (arr[i] == value)
            return true;

    return false;
}

void addStop(int* arr, int* count, int value) {

    if (!contains(arr, *count, value)) {
        arr[*count] = value;
        (*count)++;
    }
}

void removeStop(int* arr, int* count, int value) {

    for (int i = 0; i < *count; i++) {

        if (arr[i] == value) {

            for (int j = i; j < *count - 1; j++)
                arr[j] = arr[j + 1];

            (*count)--;
            return;
        }
    }
}

bool canBoard(Elevator* e) {
    return e->passengerCount < capacity;
}

bool hasUp(Elevator* e) {

    for (int i = 0; i < e->passengerCount; i++)
        if (e->passengers[i]->end > e->currFloor)
            return true;

    return false;
}

bool hasDown(Elevator* e) {

    for (int i = 0; i < e->passengerCount; i++)
        if (e->passengers[i]->end < e->currFloor)
            return true;

    return false;
}

double calculateScore(Elevator* e, int targetFloor, bool goingUp){
    int totalStops = e->upCount + e->downCount;
    if(e->passengerCount >= capacity){
        return 1e9;
    }
    if (e->state == IDLE) {
        if (totalStops >= 8) return 1e9;
        return abs(e->currFloor - targetFloor);
    }

    if (totalStops >= 15)
        return 1e9;

    if (e->state == UP && goingUp && targetFloor >= e->currFloor)
        return (targetFloor - e->currFloor) + e->passengerCount * 2 + totalStops * 10;

    if (e->state == DOWN && !goingUp && targetFloor <= e->currFloor)
        return (e->currFloor - targetFloor) + e->passengerCount * 2 + totalStops * 10;

    return 1e9;
}

void unload(Elevator* e, int time){
    for(int i=0; i<e->passengerCount; i++){
        Passenger* p = e->passengers[i];
        if(p->end ==e->currFloor){
            p->exitTime=time;
            totalWait+=getWait(p);
            totalTravel+=getTotalTime(p);
            numPassengers++;
            for(int j=i; j<e->passengerCount-1; j++){
                e->passengers[j]=e->passengers[j+1];
            }
            e->passengerCount--;
            i--;
        }
    }
}