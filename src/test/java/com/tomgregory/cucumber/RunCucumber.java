package com.tomgregory.cucumber;

import io.cucumber.core.cli.Main;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.presentation.PresentationMode;
import net.masterthought.cucumber.sorting.SortingMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RunCucumber {

  private final static String REPORTS_PATH = "build/GridReports";
  private final static int MAX_THREAD_COUNT = 10;

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    List<List<String>> allExecutionArguments = getArguments(List.of(args));
    System.out.println("allExecutionArguments-"+allExecutionArguments);
    if (allExecutionArguments.isEmpty()) {
      throw new IllegalArgumentException("No matching scenarios were found to run Execution");
    }
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    List<Future<Byte>> futures = allExecutionArguments.stream().map(theseArgs -> executorService.submit(() -> {
      System.out.println("Starting CucumberLauncher execution");
      return Main.run(theseArgs.toArray(new String[]{}));
    })).toList();

    List<Byte> exitStatuses = waitForCompletion(futures);
    new GenerateReport().mergeReports();
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
    boolean rerunFlag = Boolean.parseBoolean(System.getProperty("rerun"));
    if (rerunFlag) {
      for (int i = 1; i<=MAX_THREAD_COUNT; i++) {
        if (new File("%s/Reports%s/rerun.txt".formatted(REPORTS_PATH, i)).exists()) {
          argsCopy.set(args.indexOf("runtimeFeatures"), "@%s/Reports%s/rerun.txt".formatted(REPORTS_PATH, i));
          argsCopy.set(args.indexOf("runtimeHtml"), "html:%s/Reports%s/cucumber-report.html".formatted(REPORTS_PATH, i));
          argsCopy.set(args.indexOf("runtimeRerun"), "rerun:%s/Reports%s/rerun.txt".formatted(REPORTS_PATH, i));
          argsCopy.set(args.indexOf("runtimeJson"), "json:%s/Reports%s/jsonReport.json".formatted(REPORTS_PATH, i));
          int threadCount = StringUtils.isBlank(System.getProperty("threads"+i)) ? 1 : Integer.parseInt(System.getProperty("threads"+i));
          argsCopy.set(args.indexOf("runtimeThreads"), String.valueOf(threadCount));
          allExecutionArguments.add(new ArrayList<>(argsCopy));
        }
      }
    } else {
      for (int i = 1; i<=MAX_THREAD_COUNT; i++) {
        if (!StringUtils.isBlank(System.getProperty("tags"+i))) {
          argsCopy.set(args.indexOf("runtimeTag"), System.getProperty("tags"+i));
          argsCopy.set(args.indexOf("runtimeHtml"), "html:%s/Reports%s/cucumber-report.html".formatted(REPORTS_PATH, i));
          argsCopy.set(args.indexOf("runtimeRerun"), "rerun:%s/Reports%s/rerun.txt".formatted(REPORTS_PATH, i));
          argsCopy.set(args.indexOf("runtimeJson"), "json:%s/Reports%s/jsonReport.json".formatted(REPORTS_PATH, i));
          int threadCount = StringUtils.isBlank(System.getProperty("threads"+i)) ? 1 : Integer.parseInt(System.getProperty("threads"+i));
          argsCopy.set(args.indexOf("runtimeThreads"), String.valueOf(threadCount));
          allExecutionArguments.add(new ArrayList<>(argsCopy));
        }
      }
    }
    return allExecutionArguments;
  }

  private static final class GenerateReport {

    public void mergeReports() {
      {
        File jsonReportsDirectory = new File(REPORTS_PATH);
        if (jsonReportsDirectory.exists()) {
          try {
            List<String> jsonFiles = new ArrayList<>();
            List<File> files = (List<File>) FileUtils.listFiles(jsonReportsDirectory, new String[]{"json"}, true);
            for (File file : files) {
              jsonFiles.add(file.getCanonicalPath());
            }

            String buildNumber = "1";
            String projectName = "Cucumber";
            Configuration configuration = new Configuration(jsonReportsDirectory, projectName);
            configuration.setBuildNumber(buildNumber);

            configuration.addClassifications("Browser", "Chrome");
            configuration.addClassifications("Branch", "master");
            configuration.setSortingMethod(SortingMethod.NATURAL);
            configuration.addPresentationModes(PresentationMode.EXPAND_ALL_STEPS);
            configuration.setTrendsStatsFile(new File(REPORTS_PATH+"/jenkins.json"));
            ReportBuilder reportBuilder = new ReportBuilder(jsonFiles, configuration);
            reportBuilder.generateReports();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }


    }

  }


}
