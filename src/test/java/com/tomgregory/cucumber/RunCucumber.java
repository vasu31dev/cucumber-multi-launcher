package com.tomgregory.cucumber;

import io.cucumber.core.cli.Main;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RunCucumber {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    List<List<String>> allExecutionArguments = getArguments(List.of(args));
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    List<Future<Byte>> futures = allExecutionArguments.stream().map(theseArgs -> executorService.submit(() -> {
      System.out.println("Starting CucumberLauncher execution");
      return Main.run(theseArgs.toArray(new String[]{}));
    })).toList();

    List<Byte> exitStatuses = waitForCompletion(futures);
    exitStatuses.stream().filter(exitStatus -> exitStatus != 0).findFirst().ifPresent(System::exit);
    System.exit(0);
  }

  private static List<Byte> waitForCompletion(List<Future<Byte>> futures) throws InterruptedException, ExecutionException {
    List<Byte> exitStatuses = new ArrayList<>();
    for (Future<Byte> future : futures) {
      exitStatuses.add(future.get());
    }
    return exitStatuses;
  }

  private static List<List<String>> getArguments(List<String> args) {
    List<String> argsCopy = new ArrayList<>(args);
    List<List<String>> allExecutionArguments = new ArrayList<>();
    for (int i = 1; i<=10; i++) {
      if (!StringUtils.isBlank(System.getProperty("tags"+i))) {
        argsCopy.set(args.indexOf("runtimeTag"), System.getProperty("tags"+i));
        int threadCount = StringUtils.isBlank(System.getProperty("threads"+i)) ? 1 : Integer.parseInt(System.getProperty("threads"+i));
        argsCopy.set(args.indexOf("runtimeThreads"), String.valueOf(threadCount));
        argsCopy.set(args.indexOf("runtimeHtml"), "html:build/GridReports/Reports%s/cucumber-report.html".formatted(i));
        argsCopy.set(args.indexOf("runtimeRerun"), "rerun:build/GridReports/Reports%s/rerun.txt".formatted(i));
        argsCopy.set(args.indexOf("runtimeJson"), "json:build/GridReports/Reports%s/jsonReport.json".formatted(i));
        allExecutionArguments.add(new ArrayList<>(argsCopy));
      }
    }
    return allExecutionArguments;
  }


}
