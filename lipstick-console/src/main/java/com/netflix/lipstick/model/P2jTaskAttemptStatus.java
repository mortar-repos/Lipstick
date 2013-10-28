package com.netflix.lipstick.model;

import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Container for the status of one task attempt for an individual task
 * @author mroddy
 *
 */
@Entity
public class P2jTaskAttemptStatus {

    private Map<String, P2jCounters> counters;
    private String taskAttemptId;
    private int runTime;
    private String status;
    private long id;
    // private long startTime;
    // private float progress;
    // private String state;
    // private long id;

    /**
     * Initialize an empty P2jTaskattemptstatus object.
     */
    public P2jTaskAttemptStatus() {
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToMany(cascade = CascadeType.ALL)
    public Map<String, P2jCounters> getCounters() {
        return counters;
    }

    public void setCounters(Map<String, P2jCounters> counters) {
        this.counters = counters;
    }

    public String getTaskAttemptId() {
        return taskAttemptId;
    }

    public void setTaskAttemptId(String taskAttemptId) {
        this.taskAttemptId = taskAttemptId;
    }

    public int getRunTime() {
        return runTime;
    }

    public void setRunTime(int runTime) {
        this.runTime = runTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
