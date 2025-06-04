package com.fa1_3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class PrimaryController {
    @FXML private TableView<Process> TableData;
    @FXML private TableColumn<Process, String> ProcessIDTable;
    @FXML private TableColumn<Process, Integer> ArrivalTimeTable;
    @FXML private TableColumn<Process, Integer> BurstTimeTable;
    @FXML private TableColumn<Process, Integer> StartTimeTable;
    @FXML private TableColumn<Process, Integer> CompletionTimeTable;
    @FXML private TableColumn<Process, Integer> TurnAroundTimeTable;
    @FXML private TableColumn<Process, Integer> WaitingTImeTable;

    @FXML private HBox ganttChart;
    @FXML private VBox ganttChart1second;
    @FXML private Text CurrentProcessId;

    @FXML private HBox ReadyQueue;
    @FXML private HBox currentQueueNumber;
    @FXML private Text QueueNumber;

    @FXML private Text currentProcessSimData;
    @FXML private Text currentTimeSimData;
    @FXML private Text AverageWaitingTimeSimData;
    @FXML private Text AverageTurnaroundTimeSimData;

    @FXML private TextField NumberOfJobsInp;
    @FXML private TextField BurstTimeInp;

    @FXML private Button runbtn;

    private ObservableList<Process> processList = FXCollections.observableArrayList();
    private Random random = new Random();
    private int nextId = 1;

    private static final int TICK_DURATION_MS = 300;
    private static final int ARRIVAL_BOUND = 10;
    private static final int BURST_BOUND = 10;


    // A small array of distinct colors (as CSS hex or named colors). Feel free to expand this list.
    private final String[] COLOR_PALETTE = {
        "#ff4c4c",   // bright red
        "#4c9aff",   // bright blue
        "#4cff4c",   // bright green
        "#ffaa4c",   // orange
        "#d24cff",   // purple
        "#4cffda",   // teal
        "#ffd24c",   // gold
        "#ff4ce0",   // pink
        "#4c4cff",   // indigo
        "#4cff96"    // mint
    };

    // Map each PID → one color from the palette
    private Map<String,String> pidColorMap = new HashMap<>();

    // Index into COLOR_PALETTE for the next unseen PID
    private int nextColorIndex = 0;

    @FXML
    public void initialize() {
        TableData.setItems(processList);

        ProcessIDTable.setCellValueFactory(cell -> cell.getValue().processIdProperty());
        ArrivalTimeTable.setCellValueFactory(cell -> cell.getValue().arrivalTimeProperty().asObject());
        BurstTimeTable.setCellValueFactory(cell -> cell.getValue().burstTimeProperty().asObject());
        StartTimeTable.setCellValueFactory(cell -> cell.getValue().startTimeProperty().asObject());
        CompletionTimeTable.setCellValueFactory(cell -> cell.getValue().completionTimeProperty().asObject());
        TurnAroundTimeTable.setCellValueFactory(cell -> cell.getValue().turnaroundTimeProperty().asObject());
        WaitingTImeTable.setCellValueFactory(cell -> cell.getValue().waitingTimeProperty().asObject());
    }

    @FXML
    private void onRun(ActionEvent event) {
        String numTxt = NumberOfJobsInp.getText().trim();
        int numJobs;
        try {
            numJobs = Integer.parseInt(numTxt);
            if (numJobs <= 0) {
                return;
            }
        } catch (NumberFormatException e) {
            return;
        }

        String burstTxt = BurstTimeInp.getText().trim();
        boolean burstIsRandom = burstTxt.equalsIgnoreCase("random");
        int fixedBurst = 0;
        if (!burstIsRandom) {
            try {
                fixedBurst = Integer.parseInt(burstTxt);
                if (fixedBurst <= 0) {
                    burstIsRandom = true;
                }
            } catch (NumberFormatException e) {
                burstIsRandom = true;
            }
        }

        // Clear previous state
        processList.clear();
        ganttChart.getChildren().clear();
        ReadyQueue.getChildren().clear();
        currentProcessSimData.setText("");
        currentTimeSimData.setText("");
        AverageWaitingTimeSimData.setText("");
        AverageTurnaroundTimeSimData.setText("");
        nextId = 1;

        // 1) Generate each Process with a random arrival in [0..ARRIVAL_BOUND-1]
        for (int i = 0; i < numJobs; i++) {
            String pid = "P" + nextId++;
            int arrival = random.nextInt(ARRIVAL_BOUND);
            int burst = burstIsRandom
                    ? (random.nextInt(BURST_BOUND) + 1)
                    : fixedBurst;

            Process p = new Process(pid, arrival, burst);
            processList.add(p);
        }

        // 2) Sort by arrival time (so Table shows them in ascending arrival order)
        processList.sort(Comparator.comparingInt(Process::getArrivalTime));


        // ── INSERT THIS BLOCK TO SHIFT ALL ARRIVALS SO THE EARLIEST IS AT 0 ───────────────
        if (!processList.isEmpty()) {
            // Find the minimum arrival among all processes
            int minArrival = processList.stream()
                                    .mapToInt(Process::getArrivalTime)
                                    .min()
                                    .orElse(0);

            // Subtract minArrival from each process’s arrivalTimeProperty
            for (Process p : processList) {
                int shifted = p.getArrivalTime() - minArrival;
                p.arrivalTimeProperty().set(shifted);
            }
        }
        // ────────────────────────────────────────────────────────────────────────────────


        // 3) (Re-)sort by the new (shifted) arrival time, just in case
        processList.sort(Comparator.comparingInt(Process::getArrivalTime));

        // 4) Build timeline & animate as before
        List<Snapshot> timeline = buildTimeline(new ArrayList<>(processList));
        playTimeline(timeline);
    }


        private List<Snapshot> buildTimeline(List<Process> procs) {
            int N = procs.size();
            int remainingCount = N;
            int currentTime = 0;
            List<Snapshot> snapshots = new ArrayList<>();

            while (remainingCount > 0) {
                // 1) Copy currentTime into a final (effectively final) variable for lambdas:
                int time = currentTime;

                // 2) Build a list of processes that have arrived and are not finished:
                List<Process> readyList = procs.stream()
                        .filter(p -> p.getArrivalTime() <= time && p.getRemainingTime() > 0)
                        .collect(Collectors.toList());

                if (readyList.isEmpty()) {
                    snapshots.add(new Snapshot(time, "Idle", new ArrayList<>()));
                    currentTime++;
                    continue;
                }

                // 3) Pick the one with the smallest remainingTime:
                Process toRun = readyList.stream()
                        .min(Comparator
                                .comparingInt(Process::getRemainingTime)
                                .thenComparingInt(Process::getArrivalTime))
                        .orElse(null);

                if (toRun == null) {
                    snapshots.add(new Snapshot(time, "Idle", new ArrayList<>()));
                    currentTime++;
                    continue;
                }

                // 4) If it’s the first time toRun is ever run, set its startTime property:
                if (toRun.getStartTime() == 0) {
                    toRun.startTimeProperty().set(time);
                }

                // 5) “Execute” it for exactly one tick → decrement remainingTime property
                int rem = toRun.getRemainingTime() - 1;
                toRun.remainingTimeProperty().set(rem);

                // 6) Build the list of waiting PIDs (arrived, not finished, not the one we’re running).
                //    Again capture currentTime in a final variable for the lambda:
                int time2 = time;
                List<String> waitingPIDs = procs.stream()
                        .filter(p -> p.getArrivalTime() <= time2 && p.getRemainingTime() > 0 && p != toRun)
                        .map(Process::getProcessId)
                        .collect(Collectors.toList());

                // 7) Record a snapshot for this tick
                snapshots.add(new Snapshot(time, toRun.getProcessId(), waitingPIDs));

                // 8) If toRun just finished (remainingTime == 0), record its completionTime property
                if (rem == 0) {
                    toRun.completionTimeProperty().set(time + 1); // finishes at end of this tick
                    remainingCount--;
                }

                // 9) Advance time by one
                currentTime++;
            }

            // 10) Once all are done, compute turnaroundTime & waitingTime for each process:
            for (Process p : procs) {
                int tat = p.getCompletionTime() - p.getArrivalTime();
                int wt  = tat - p.getBurstTime();
                p.turnaroundTimeProperty().set(tat);
                p.waitingTimeProperty().set(wt);
            }

            return snapshots;
        }


    private void playTimeline(List<Snapshot> timeline) {
        int totalTicks = timeline.size();

        // Clear any old blocks (though onRun already cleared them)
        ganttChart.getChildren().clear();
        ReadyQueue.getChildren().clear();

        Timeline anim = new Timeline();
        anim.setCycleCount(1);  // Play once through

        for (int i = 0; i < totalTicks; i++) {
            final int tickIndex = i;
            KeyFrame kf = new KeyFrame(
                Duration.millis(tickIndex * TICK_DURATION_MS),
                ae -> {
                    Snapshot snap = timeline.get(tickIndex);

                    // ─── Determine the color for this runningProcess ─────────────────
                    String pid = snap.runningProcess;
                    String color = pidColorMap.computeIfAbsent(pid, key -> {
                        // If we've exhausted palette, wrap around
                        String c = COLOR_PALETTE[nextColorIndex % COLOR_PALETTE.length];
                        nextColorIndex++;
                        return c;
                    });

                    // ─── Build Gantt‐chart block with that background color ──────────
                    VBox block = new VBox();
                    block.setPrefWidth(ganttChart1second.getPrefWidth());
                    block.setPrefHeight(ganttChart1second.getPrefHeight());

                    // Inline CSS: border style copied from prototype, background from our color
                    block.setStyle(
                        "-fx-alignment: center; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-border-color: black; " +
                        "-fx-border-width: 1;"
                    );

                    // Put the PID text inside
                    Text pidText = new Text(pid);
                    pidText.setFill(CurrentProcessId.getFill());
                    pidText.setStyle(CurrentProcessId.getStyle());
                    block.getChildren().add(pidText);

                    ganttChart.getChildren().add(block);

                    // ─── Update “current running” and “current time” ────────────────
                    currentProcessSimData.setText("Running: " + pid);
                    currentTimeSimData.setText("Time: " + (snap.time + 1));

                    // ─── Rebuild ReadyQueue ────────────────────────────────────────
                    ReadyQueue.getChildren().clear();
                    for (String waitingPid : snap.waitingPIDs) {
                        // If this waitingPid has never been assigned a color, we assign it here too:
                        pidColorMap.computeIfAbsent(waitingPid, key -> {
                            String c = COLOR_PALETTE[nextColorIndex % COLOR_PALETTE.length];
                            nextColorIndex++;
                            return c;
                        });

                        HBox queueBox = new HBox();
                        queueBox.setStyle("-fx-background-color: " + pidColorMap.get(waitingPid) + "; " +
                                        "-fx-alignment: center; -fx-padding: 2; -fx-border-color: black; -fx-border-width: 1;");
                        queueBox.setPrefHeight(currentQueueNumber.getPrefHeight());
                        queueBox.setPrefWidth(currentQueueNumber.getPrefWidth());

                        Text txt = new Text(waitingPid);
                        txt.setStyle(QueueNumber.getStyle());
                        queueBox.getChildren().add(txt);

                        ReadyQueue.getChildren().add(queueBox);
                    }
                }
            );
            anim.getKeyFrames().add(kf);
        }

        // Final KeyFrame: compute & display averages, refresh table
        KeyFrame showAverages = new KeyFrame(
            Duration.millis(totalTicks * TICK_DURATION_MS + 1),
            ae -> {
                double sumWT = 0, sumTAT = 0;
                int N = processList.size();
                for (Process p : processList) {
                    sumWT  += p.getWaitingTime();
                    sumTAT += p.getTurnaroundTime();
                }
                double avgWT  = sumWT / N;
                double avgTAT = sumTAT / N;

                AverageWaitingTimeSimData.setText(String.format("Avg WT: %.2f", avgWT));
                AverageTurnaroundTimeSimData.setText(String.format("Avg TAT: %.2f", avgTAT));

                TableData.refresh();
            }
        );
        anim.getKeyFrames().add(showAverages);

        anim.play();
    }



    private static class Snapshot {
        final int time;
        final String runningProcess;
        final List<String> waitingPIDs;

        Snapshot(int time, String runningProcess, List<String> waitingPIDs) {
            this.time = time;
            this.runningProcess = runningProcess;
            this.waitingPIDs = waitingPIDs;
        }
    }
}
