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
    // ──── 1. TableView + its 7 columns ────────────────────────────────────────────────
    @FXML private TableView<Process> TableData;
    @FXML private TableColumn<Process, String>  ProcessIDTable;
    @FXML private TableColumn<Process, Integer> ArrivalTimeTable;
    @FXML private TableColumn<Process, Integer> BurstTimeTable;
    @FXML private TableColumn<Process, Integer> StartTimeTable;
    @FXML private TableColumn<Process, Integer> CompletionTimeTable;
    @FXML private TableColumn<Process, Integer> TurnAroundTimeTable;
    @FXML private TableColumn<Process, Integer> WaitingTImeTable;

    // ──── 2. Gantt chart container (HBox) + one prototype VBox/text ───────────────────
    @FXML private HBox ganttChart;
    @FXML private VBox ganttChart1second;
    @FXML private Text CurrentProcessId;

    // ──── 3. Ready‐queue container (HBox) + one prototype HBox/text ─────────────────
    @FXML private HBox ReadyQueue;
    @FXML private HBox currentQueueNumber;
    @FXML private Text QueueNumber;

    // ──── 4. Text nodes for dynamic simulation data ─────────────────────────────────
    @FXML private Text currentProcessSimData;
    @FXML private Text currentTimeSimData;
    @FXML private Text AverageWaitingTimeSimData;
    @FXML private Text AverageTurnaroundTimeSimData;

    // ──── 5. User inputs: number of jobs, burst time or “random” ─────────────────────
    @FXML private TextField NumberOfJobsInp;
    @FXML private TextField BurstTimeInp;

    // ──── 6. Run button (wired to onRun) ────────────────────────────────────────────
    @FXML private Button runbtn;

    // ──── 7. Internal fields: list of processes, random generator, ID counter ──────
    private ObservableList<Process> processList = FXCollections.observableArrayList();
    private Random random = new Random();
    private int nextId = 1;

    // ──── Constants: modify as you like ───────────────────────────────────────────────
    private static final int TICK_DURATION_MS = 300; // ms per simulated tick
    private static final int ARRIVAL_BOUND   = 10;   // arrivals ∈ [0..9]
    private static final int BURST_BOUND     = 10;   // random burst ∈ [1..10]

    // ──── Color palette for Gantt chart ─────────────────────────────────────────────
    private final String[] COLOR_PALETTE = {
        "#ff4c4c", "#4c9aff", "#4cff4c", "#ffaa4c", "#d24cff",
        "#4cffda", "#ffd24c", "#ff4ce0", "#4c4cff", "#4cff96"
    };
    // Map each PID (or "Idle") → assigned color
    private Map<String, String> pidColorMap = new HashMap<>();
    // Next index in palette for a newly seen PID
    private int nextColorIndex = 0;

    // ──── initialize(): wire TableView columns to Process properties ────────────────
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

    // ──── onRun: triggered by clicking “Run” ───────────────────────────────────────────
    @FXML
    private void onRun(ActionEvent event) {
        // 1) Parse “Number of Jobs”
        String numTxt = NumberOfJobsInp.getText().trim();
        int numJobs;
        try {
            numJobs = Integer.parseInt(numTxt);
            if (numJobs <= 0) return;  // Must be positive
        } catch (NumberFormatException e) {
            return; // invalid input
        }

        // 2) Parse “Burst Time” (either fixed integer or “random”)
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

        // 3) Clear previous state, including color‐map
        processList.clear();
        pidColorMap.clear();
        nextColorIndex = 0;

        ganttChart.getChildren().clear();
        ReadyQueue.getChildren().clear();
        currentProcessSimData.setText("");
        currentTimeSimData.setText("");
        AverageWaitingTimeSimData.setText("");
        AverageTurnaroundTimeSimData.setText("");
        nextId = 1;

        // 4) Generate each Process with random arrival ∈ [0..ARRIVAL_BOUND−1]
        for (int i = 0; i < numJobs; i++) {
            String pid = "P" + nextId++;
            int arrival = random.nextInt(ARRIVAL_BOUND);
            int burst   = burstIsRandom
                        ? (random.nextInt(BURST_BOUND) + 1)
                        : fixedBurst;

            Process p = new Process(pid, arrival, burst);
            processList.add(p);
        }

        // 5) Sort by arrival time (so smallest arrival is first)
        processList.sort(Comparator.comparingInt(Process::getArrivalTime));

        // 6) SHIFT arrivals so that the earliest becomes 0
        if (!processList.isEmpty()) {
            int minArrival = processList.stream()
                                        .mapToInt(Process::getArrivalTime)
                                        .min()
                                        .orElse(0);
            for (Process p : processList) {
                int shifted = p.getArrivalTime() - minArrival;
                p.arrivalTimeProperty().set(shifted);
            }
        }

        // 7) Re‐sort by the (shifted) arrival times
        processList.sort(Comparator.comparingInt(Process::getArrivalTime));

        // 8) Build the SRTF timeline and animate
        List<Snapshot> timeline = buildTimeline(new ArrayList<>(processList));
        playTimeline(timeline);
    }

    // ──── Build a tick‐by‐tick SRTF timeline ─────────────────────────────────────────
    private List<Snapshot> buildTimeline(List<Process> procs) {
        int N = procs.size();
        int remainingCount = N;
        int currentTime = 0;
        List<Snapshot> snapshots = new ArrayList<>();

        while (remainingCount > 0) {
            int time = currentTime;  // effectively final for lambdas

            // 1) Find all arrived & unfinished processes
            List<Process> readyList = procs.stream()
                .filter(p -> p.getArrivalTime() <= time && p.getRemainingTime() > 0)
                .collect(Collectors.toList());

            if (readyList.isEmpty()) {
                // CPU idle this tick
                snapshots.add(new Snapshot(time, "Idle", new ArrayList<>()));
                currentTime++;
                continue;
            }

            // 2) Pick process with smallest remainingTime (tie → smallest arrivalTime)
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

            // 3) If first time running, record startTime = time (since default was –1)
            if (toRun.getStartTime() < 0) {
                toRun.startTimeProperty().set(time);
            }

            // 4) Execute for one tick → decrement remainingTime
            int rem = toRun.getRemainingTime() - 1;
            toRun.remainingTimeProperty().set(rem);

            // 5) Build waiting‐queue list (arrived, not finished, not toRun)
            int time2 = time;
            List<String> waitingPIDs = procs.stream()
                .filter(p ->
                    p.getArrivalTime() <= time2 &&
                    p.getRemainingTime() > 0 &&
                    p != toRun
                )
                .map(Process::getProcessId)
                .collect(Collectors.toList());

            // 6) Record snapshot for this tick
            snapshots.add(new Snapshot(time, toRun.getProcessId(), waitingPIDs));

            // 7) If it just finished, record completionTime = time + 1
            if (rem == 0) {
                toRun.completionTimeProperty().set(time + 1);
                remainingCount--;
            }

            currentTime++;
        }

        // 8) After all complete, compute TAT & WT for each process
        for (Process p : procs) {
            int tat = p.getCompletionTime() - p.getArrivalTime();
            int wt  = tat - p.getBurstTime();
            p.turnaroundTimeProperty().set(tat);
            p.waitingTimeProperty().set(wt);
        }

        return snapshots;
    }

    // ──── Animate the Gantt chart & ready queue based on snapshots ─────────────────
    private void playTimeline(List<Snapshot> timeline) {
        int totalTicks = timeline.size();

        // Clear any old content
        ganttChart.getChildren().clear();
        ReadyQueue.getChildren().clear();

        Timeline anim = new Timeline();
        anim.setCycleCount(1);  // Play once through all keyframes

        for (int i = 0; i < totalTicks; i++) {
            final int tickIndex = i;
            KeyFrame kf = new KeyFrame(
                Duration.millis(tickIndex * TICK_DURATION_MS),
                ae -> {
                    Snapshot snap = timeline.get(tickIndex);

                    // 1) Assign or reuse a color for runningProcess (could be "Idle")
                    String pid = snap.runningProcess;
                    String color = pidColorMap.computeIfAbsent(pid, key -> {
                        String c = COLOR_PALETTE[nextColorIndex % COLOR_PALETTE.length];
                        nextColorIndex++;
                        return c;
                    });

                    // 2) Build one VBox block in the Gantt chart
                    VBox block = new VBox();
                    block.setPrefWidth(ganttChart1second.getPrefWidth());
                    block.setPrefHeight(ganttChart1second.getPrefHeight());
                    block.setStyle(
                        "-fx-alignment: center; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-border-color: black; -fx-border-width: 1;"
                    );

                    Text pidText = new Text(pid);
                    pidText.setFill(CurrentProcessId.getFill());
                    pidText.setStyle(CurrentProcessId.getStyle());
                    block.getChildren().add(pidText);

                    ganttChart.getChildren().add(block);

                    // 3) Update “current running” and “current time” labels
                    currentProcessSimData.setText("Running: " + pid);
                    currentTimeSimData.setText("Time: " + (snap.time + 1));

                    // 4) Rebuild the ReadyQueue area
                    ReadyQueue.getChildren().clear();
                    for (String waitingPid : snap.waitingPIDs) {
                        // Ensure each waiting PID has a color assigned
                        pidColorMap.computeIfAbsent(waitingPid, key -> {
                            String c = COLOR_PALETTE[nextColorIndex % COLOR_PALETTE.length];
                            nextColorIndex++;
                            return c;
                        });

                        HBox queueBox = new HBox();
                        queueBox.setStyle(
                            "-fx-background-color: " + pidColorMap.get(waitingPid) + "; " +
                            "-fx-alignment: center; -fx-padding: 2; " +
                            "-fx-border-color: black; -fx-border-width: 1;"
                        );
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

        // Final KeyFrame: after all ticks, compute + display averages, refresh table
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

    // ──── Helper class: one snapshot of the system at a given tick ─────────────────
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
