package controller;



import io.camunda.tasklist.dto.Task;
import io.camunda.tasklist.dto.TaskList;
import io.camunda.tasklist.dto.TaskState;
import io.camunda.tasklist.exception.TaskListException;
import io.camunda.client.CamundaClient;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.TaskListService;


@RestController
@RequestMapping("/parking")
public class ParkingTaskController {

    private static final Logger LOG = LoggerFactory.getLogger(ParkingTaskController.class);
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    @Autowired
    private TaskListService taskListService;

    @Autowired
    private CamundaClient client;

    @GetMapping("/tasks")
    public ResponseEntity<TaskList> getParkingTasks(@RequestParam(required = false) String filter) {
        TaskList tasks = new TaskList();
        LOG.info("Fetching parking tasks...");

        try {
            tasks = taskListService.getTaskList(TaskState.CREATED, null);
            LOG.info("Tasks fetched: {}", tasks.size());

            for (Task task : tasks) {
                LOG.info("Task ID: {}", task.getId());
            }

        } catch (Exception e) {
            LOG.error("Error fetching tasks", e);
        }

        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @PostMapping("/complete/{taskId}")
    public void completeParkingTask(@PathVariable String taskId,
                                    @RequestBody Map<String, Object> variables) throws TaskListException {

        LOG.info("Completing task {} with variables: {}", taskId, variables);

        if (variables.containsKey("lastComment")) {
            String comment = (String) variables.get("lastComment");
            Object commentsVar = variables.get("comments");
            List<Map<String, String>> comments = null;

            Map<String, String> commentToAdd = Map.of(
                    "author", "Paweł Drwal",
                    "comment", comment,
                    "date", sdf.format(new Date())
            );

            if (commentsVar == null || "".equals(commentsVar)) {
                comments = List.of(commentToAdd);
            } else {
                comments = (List<Map<String, String>>) commentsVar;
                comments.add(commentToAdd);
            }

            variables.put("comments", comments);
            variables.put("decision", "yes");
        }

        taskListService.completeTask(taskId, variables);
        LOG.info("Task {} completed.", taskId);
    }
}