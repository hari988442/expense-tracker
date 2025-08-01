// SmartTaskScheduler Main Application

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.collections.*;
import java.time.LocalDate;
import java.util.*;
import java.io.*;
import com.google.gson.*;

public class SmartTaskScheduler extends Application {

    private PriorityQueue<Task> taskQueue = new PriorityQueue<>();
    private ObservableList<Task> taskList = FXCollections.observableArrayList();
    private ListView<Task> listView = new ListView<>(taskList);
    private FileStorage storage = new FileStorage();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        loadTasks();

        TextField titleField = new TextField();
        titleField.setPromptText("Task Title");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("High", "Medium", "Low");
        priorityBox.getSelectionModel().selectFirst();

        DatePicker datePicker = new DatePicker(LocalDate.now());

        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> {
            String title = titleField.getText();
            int priority = priorityBox.getSelectionModel().getSelectedIndex() + 1;
            LocalDate deadline = datePicker.getValue();
            if (!title.isEmpty() && deadline != null) {
                Task task = new Task(title, priority, deadline);
                taskQueue.add(task);
                taskList.add(task);
                ReminderService.scheduleReminder(task);
                saveTasks();
                titleField.clear();
            }
        });

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setOnAction(e -> {
            Task selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                taskQueue.remove(selected);
                taskList.remove(selected);
                saveTasks();
            }
        });

        VBox inputBox = new VBox(10, titleField, priorityBox, datePicker, addButton, deleteButton);
        inputBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setLeft(inputBox);
        root.setCenter(listView);

        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setTitle("Smart Task Scheduler");
        primaryStage.show();
    }

    private void saveTasks() {
        storage.saveToFile(new ArrayList<>(taskList));
    }

    private void loadTasks() {
        List<Task> loaded = storage.loadFromFile();
        taskQueue.addAll(loaded);
        taskList.addAll(loaded);
        for (Task t : loaded) ReminderService.scheduleReminder(t);
    }
}

// Task.java
class Task implements Comparable<Task> {
    private String title;
    private int priority; // 1 = High, 2 = Medium, 3 = Low
    private LocalDate deadline;

    public Task(String title, int priority, LocalDate deadline) {
        this.title = title;
        this.priority = priority;
        this.deadline = deadline;
    }

    public String getTitle() { return title; }
    public int getPriority() { return priority; }
    public LocalDate getDeadline() { return deadline; }

    @Override
    public int compareTo(Task o) {
        if (this.priority != o.priority)
            return Integer.compare(this.priority, o.priority);
        return this.deadline.compareTo(o.deadline);
    }

    @Override
    public String toString() {
        return title + " | " + deadline + " | " + (priority == 1 ? "High" : priority == 2 ? "Medium" : "Low");
    }
}

// ReminderService.java
class ReminderService {
    public static void scheduleReminder(Task task) {
        Timer timer = new Timer();
        TimerTask reminder = new TimerTask() {
            public void run() {
                System.out.println("Reminder: " + task.getTitle() + " due on " + task.getDeadline());
            }
        };
        long delay = java.time.Duration.between(LocalDate.now().atStartOfDay(), task.getDeadline().atStartOfDay()).toMillis();
        if (delay > 0) timer.schedule(reminder, delay);
    }
}

// FileStorage.java
class FileStorage {
    private final String FILE_NAME = "tasks.json";

    public void saveToFile(List<Task> tasks) {
        try (Writer writer = new FileWriter(FILE_NAME)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(tasks, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Task> loadFromFile() {
        try (Reader reader = new FileReader(FILE_NAME)) {
            Gson gson = new Gson();
            Task[] tasks = gson.fromJson(reader, Task[].class);
            return tasks != null ? new ArrayList<>(Arrays.asList(tasks)) : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}

