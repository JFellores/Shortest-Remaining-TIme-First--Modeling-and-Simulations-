package com.fa1_3;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class for one Process/Job in the SRTF simulator.
 * We initialize startTime and completionTime to –1, so that "0" is a valid recorded start.
 */
public class Process {
    private final StringProperty processId;
    private final IntegerProperty arrivalTime;
    private final IntegerProperty burstTime;
    private final IntegerProperty remainingTime;
    private final IntegerProperty startTime;
    private final IntegerProperty completionTime;
    private final IntegerProperty turnaroundTime;
    private final IntegerProperty waitingTime;

    public Process(String pid, int arrival, int burst) {
        this.processId      = new SimpleStringProperty(pid);
        this.arrivalTime    = new SimpleIntegerProperty(arrival);
        this.burstTime      = new SimpleIntegerProperty(burst);
        this.remainingTime  = new SimpleIntegerProperty(burst);

        // Initialize to –1 so that a real start at time 0 is distinguishable:
        this.startTime      = new SimpleIntegerProperty(-1);
        this.completionTime = new SimpleIntegerProperty(-1);

        this.turnaroundTime = new SimpleIntegerProperty(0);
        this.waitingTime    = new SimpleIntegerProperty(0);
    }

    // Property getters (used by TableColumn cell-value factories)
    public StringProperty processIdProperty()        { return processId; }
    public IntegerProperty arrivalTimeProperty()     { return arrivalTime; }
    public IntegerProperty burstTimeProperty()       { return burstTime; }
    public IntegerProperty remainingTimeProperty()   { return remainingTime; }
    public IntegerProperty startTimeProperty()       { return startTime; }
    public IntegerProperty completionTimeProperty()  { return completionTime; }
    public IntegerProperty turnaroundTimeProperty()  { return turnaroundTime; }
    public IntegerProperty waitingTimeProperty()     { return waitingTime; }

    // Convenience getters:
    public String getProcessId()     { return processId.get(); }
    public int getArrivalTime()      { return arrivalTime.get(); }
    public int getBurstTime()        { return burstTime.get(); }
    public int getRemainingTime()    { return remainingTime.get(); }
    public int getStartTime()        { return startTime.get(); }
    public int getCompletionTime()   { return completionTime.get(); }
    public int getTurnaroundTime()   { return turnaroundTime.get(); }
    public int getWaitingTime()      { return waitingTime.get(); }

    // Value setters (used during simulation):
    public void setRemainingTime(int rt)    { this.remainingTime.set(rt); }
    public void setStartTime(int st)        { this.startTime.set(st); }
    public void setCompletionTime(int ct)   { this.completionTime.set(ct); }
    public void setTurnaroundTime(int tat)  { this.turnaroundTime.set(tat); }
    public void setWaitingTime(int wt)      { this.waitingTime.set(wt); }
}
