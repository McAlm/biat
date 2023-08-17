package com.camunda.demo.biat.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

import com.camunda.demo.biat.storage.StorageService;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;

@Controller

public class Facade {

    @Autowired
    private StorageService storageService;


    @Autowired
    private ZeebeClient zeebeClient;

    @GetMapping("/start")
    public String index(Model model) {
        return "start";
    }

    @PostMapping("/startProcess")
    public String handleFileUpload(
            Model model,
            @RequestParam("file") MultipartFile file, //
            @RequestParam("employeeName") String employeeName, //
            @RequestParam("department") String department, //
            @RequestParam("email") String email) {

        System.out.println("You successfully uploaded " + file.getOriginalFilename() + " for Employee " + employeeName);
        storageService.store(file);
        ProcessInstanceEvent processInstance = zeebeClient//
            .newCreateInstanceCommand()//
            .bpmnProcessId(ProcessConstants.PROCESS_ID)//
            .latestVersion()//
            .variables(Map.of(
                "email", email, //
                "employeeName", employeeName, //
                "department", department, //
                "file", file.getOriginalFilename()))//
                .send()//
                .join();

        model.addAttribute("processInstanceId",processInstance.getProcessInstanceKey());

        return "success";
    }

    // @GetMapping("/greeting")
    // public String greetingForm(Model model) {
    // model.addAttribute("greeting", new Greeting());
    // return "greetingPage";
    // }

    // @PostMapping("/greeting")
    // public String greetingSubmit(@ModelAttribute Greeting greeting, Model model)
    // {
    // model.addAttribute("greeting", greeting);
    // return "result";
    // }
}
