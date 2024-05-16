package com.example.demo.Listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

public class CustomStepListener implements StepExecutionListener{
    @Override
    public void beforeStep(StepExecution stepExecution) {
        System.out.println("============");
        System.out.println("Step開始:" + stepExecution.getStepName());
        System.out.println("Job execute context:" + stepExecution.getJobExecution().getExecutionContext());
        System.out.println("Step exedute context:" + stepExecution.getExecutionContext());
        System.out.println("============");

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        System.out.println("===========");
        System.out.println("Step終了:" + stepExecution.getStepName());
        System.out.println("Job execute context:" + stepExecution.getJobExecution().getExecutionContext());
        System.out.println("Step exedute context:" + stepExecution.getExecutionContext());
        System.out.println("===========");
        return null;
    }
}
