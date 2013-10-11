package com.netflix.lipstick.model;

import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Container for the status of an individual task for a map/reduce job
 * @author mroddy
 *
 */
@Entity
public class P2jTaskStatus {

    private Map<String, P2jCounters> counters;
    private String taskId;
    private long finishTime;
    private long startTime;
    private float progress;
    private String state;
    private long id;

    /**
     * Initialize an empty P2jTaskStatus object.
     */
    public P2jTaskStatus() {
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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float Progress) {
        this.progress = progress;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

}
