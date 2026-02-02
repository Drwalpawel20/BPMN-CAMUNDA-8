package controller;

import io.camunda.client.CamundaClient;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/")
public class ParkingController {

    private static final Logger LOG = LoggerFactory.getLogger(ParkingController.class);

    @Autowired
    private CamundaClient client;

    @PostMapping("/start")
    public void startParkingProcess(@RequestBody Map<String, Object> variables) {

        LOG.info("Starting process ParkingProcess with variables: {}", variables);

        variables.put("applicationReceived", true);

        client
                .newCreateInstanceCommand()
                .bpmnProcessId("Process_Parking_Final") // nowy ID procesu
                .latestVersion()
                .variables(variables)
                .send();

        LOG.info("ParkingProcess started with variables: {}", variables);
    }
}
