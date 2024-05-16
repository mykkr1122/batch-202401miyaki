package com.example.demo.Listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("============");
        System.out.println("job開始:" + jobExecution.getJobInstance().getJobName());
        System.out.println("job params:" + jobExecution.getJobParameters());
        System.out.println("job execute context:" + jobExecution.getExecutionContext());
        System.out.println("============");

    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.println("============");
        System.out.println("job終了:" + jobExecution.getJobInstance().getJobName());
        System.out.println("job params:" + jobExecution.getJobParameters());
        System.out.println("job execute context:" + jobExecution.getExecutionContext());
        System.out.println("============");

    }
}
