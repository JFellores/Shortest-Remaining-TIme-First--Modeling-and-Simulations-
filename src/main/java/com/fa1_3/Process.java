package com.fa1_3;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
    this.startTime      = new SimpleIntegerProperty(0);
    this.completionTime = new SimpleIntegerProperty(0);
    this.turnaroundTime = new SimpleIntegerProperty(0);
    this.waitingTime    = new SimpleIntegerProperty(0);
  }

  // Expose each property:
  public StringProperty processIdProperty()       { return processId; }
  public IntegerProperty arrivalTimeProperty()    { return arrivalTime; }
  public IntegerProperty burstTimeProperty()      { return burstTime; }
  public IntegerProperty remainingTimeProperty()  { return remainingTime; }
  public IntegerProperty startTimeProperty()      { return startTime; }
  public IntegerProperty completionTimeProperty() { return completionTime; }
  public IntegerProperty turnaroundTimeProperty() { return turnaroundTime; }
  public IntegerProperty waitingTimeProperty()    { return waitingTime; }

  // Convenience “value” getters if needed:
  public String getProcessId()    { return processId.get(); }
  public int getArrivalTime()     { return arrivalTime.get(); }
  public int getBurstTime()       { return burstTime.get(); }
  public int getRemainingTime()   { return remainingTime.get(); }
  public int getStartTime()       { return startTime.get(); }
  public int getCompletionTime()  { return completionTime.get(); }
  public int getTurnaroundTime()  { return turnaroundTime.get(); }
  public int getWaitingTime()     { return waitingTime.get(); }
}
