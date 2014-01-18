/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.lipstick.warnings;

import java.util.Map;
import java.util.List;
import java.io.IOException;
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import org.apache.pig.tools.pigstats.JobStats;
import com.netflix.lipstick.model.P2jWarning;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.TaskReport;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class JobWarnings {

    private static final Log log = LogFactory.getLog(JobWarnings.class);

    public static final String NO_OUTPUT_RECORDS_KEY = "noOutputRecords";
    public static final String SKEWED_REDUCERS_KEY = "skewedReducers";

    /* Require that there are a minimum num of reducer tasks to consider
       a job reducer skewed to prevent a high level of false posititives
       that ensue otherwise. */
    public static final int MAX_REDUCERS_FOR_SKEW = 2;


    public boolean shouldNoOuputRecordsWarn(JobStats jobStats) {
        if (0 == jobStats.getRecordWrittern()) {
            return true;
        } else {
            return false;
        }
    }

    public static class ReducerDuration {
        public String reducerTaskId;
        public double duration;
        public ReducerDuration(String reducerTaskId, double duration) {
            this.reducerTaskId = reducerTaskId;
            this.duration = duration;
        }
    }

    /* Version 0.0 attempt, if any of the top 10% of reducers,
       in terms of duration, are more than 2x the stddev from
       the mean of the bottom 90% we consider skew to be present.
       Version 0.1 pending more data about reducer skew.  This is
       to be considered a best effort for now. */
    public List<String> findSkewedReducers(List<ReducerDuration> reducerTimes) {
        if (! (MAX_REDUCERS_FOR_SKEW < reducerTimes.size())) {
            return Lists.newLinkedList();
        }

        int numPotentialOutliers = (int)Math.ceil(reducerTimes.size() / 10.0);
        int inflection = reducerTimes.size() - numPotentialOutliers;
        List<ReducerDuration> potentialOutliers = reducerTimes.subList(inflection, reducerTimes.size());
        List<ReducerDuration> referenceReducers = reducerTimes.subList(0, inflection);

        /* List of reducer duration values that we will compare the
           potential outliers to. */
        double[] referenceDurations = new double[referenceReducers.size()];
        for (int i = 0; i < referenceReducers.size(); i++) {
            referenceDurations[i] = referenceReducers.get(i).duration;
        }

        double refMean = StatUtils.mean(referenceDurations);
        double refVariance = StatUtils.populationVariance(referenceDurations, refMean);
        double refStdDev = Math.sqrt(refVariance);

        /* If the time to complete the task is more than this far
           from the mean of all task completion times, we consider
           it skewed */
        double distToMeanThreshold  = (refStdDev * 2) + refMean;

        /* Now collect and return any of the outliers whose distance
           from the mean is great than the computed threshold. */
        List<String> skewedReducerIds = Lists.newArrayList();
        for (ReducerDuration r: potentialOutliers) {
            if ((r.duration - refMean) > distToMeanThreshold) {
                skewedReducerIds.add(r.reducerTaskId);
            }
        }
        return skewedReducerIds;
    }

    public List<ReducerDuration> enumerateReducerRunTimesAccending(JobClient jobClient, String jobId) {
        try {
            TaskReport[] reduceTasks = jobClient.getReduceTaskReports(jobId);
            return enumerateReducerRunTimesAccending(reduceTasks);
        } catch (IOException e) {
            log.error("Error getting reduce task reports, continuing", e);
            return Lists.newArrayList();
        }
    }

    /* Extract all running or completed reducer tasks for the job, their runtime and sort them
       in accending order. Used to partition reduce tasks to detect reducer skew. */
    public List<ReducerDuration> enumerateReducerRunTimesAccending(TaskReport[] reduceTasks) {
        List<ReducerDuration> reducerDurations = Lists.newArrayList();
        long now = System.currentTimeMillis() / 1000;
        for (int i = 0; i < reduceTasks.length; i++) {
            String taskId = reduceTasks[i].getTaskID().toString();
            long startTime = reduceTasks[i].getStartTime();
            long finishTime = reduceTasks[i].getFinishTime();
            if (0 == finishTime) {
                /* Job hasn't finished yet */
                finishTime = now;
            }
            if (0 != startTime) {
                reducerDurations.add(new ReducerDuration(taskId, (double)finishTime - startTime));
            }
        }
        return reducerDurations;
    }

    protected void addWarning(String jobId, Map<String, P2jWarning> warningsMap, String warningKey) {
        Map<String, String> attrs = Maps.newHashMap();
        addWarning(jobId, warningsMap, warningKey, attrs);
    }

    protected void addWarning(JobStats jobStats, Map<String, P2jWarning> warningsMap, String warningKey) {
        Map<String, String> attrs = Maps.newHashMap();
        addWarning(jobStats.getJobId(), warningsMap, warningKey, attrs);
    }

    protected void addWarning(String jobId, Map<String, P2jWarning> warningsMap, String warningKey, Map<String, String> attributes) {
        P2jWarning pw = new P2jWarning();
        pw.setWarningKey(warningKey);
        pw.setJobId(jobId);
        pw.setWarningAttributes(attributes);
        warningsMap.put(warningKey, pw);
    }

    public Map<String, P2jWarning> findCompletedJobWarnings(JobClient jobClient, JobStats jobStats) {
        Map<String, P2jWarning> warnings = findRunningJobWarnings(jobClient, jobStats.getJobId());
        if (shouldNoOuputRecordsWarn(jobStats)) {
            addWarning(jobStats, warnings, NO_OUTPUT_RECORDS_KEY);
        }
        return warnings;
    }

    public Map<String, P2jWarning> findRunningJobWarnings(JobClient jobClient, String jobId) {
        Map<String, P2jWarning> warnings = Maps.newHashMap();
        List<ReducerDuration> reducerTimes = enumerateReducerRunTimesAccending(jobClient, jobId);
        List<String> skewedReducerIds = findSkewedReducers(reducerTimes);
        if (0 < skewedReducerIds.size()) {
            /* todo: find a better why to shove a list into the attribute map
               than a csv.  I feel shame at this. */
            String sris = Joiner.on(",").join(skewedReducerIds);
                addWarning(jobId, warnings, NO_OUTPUT_RECORDS_KEY, 
                           ImmutableMap.of(SKEWED_REDUCERS_KEY, sris));
        }
        return warnings;
    }
}
