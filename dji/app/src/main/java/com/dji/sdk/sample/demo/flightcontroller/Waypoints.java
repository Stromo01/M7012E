package com.dji.sdk.sample.demo.flightcontroller;

public class Waypoints {
    private double x;
    private double y;
    private double z;
    private double w_r;
    private double x_r;
    private double y_r;
    private double z_r;
    private int id;

    private float[] waypoint_pos;

    private float[] waypoint_angle;
    // Default Constructor
    public Waypoints() {}

    // Parameterized Constructor
    public Waypoints(double x, double y, double z, double w_r, double x_r, double y_r, double z_r, int id) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w_r = w_r;
        this.x_r = x_r;
        this.y_r = y_r;
        this.z_r = z_r;
        this.id = id;
    }
    public String toString() {
        return "Waypoints{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w_r=" + w_r +
                ", x_r=" + x_r +
                ", y_r=" + y_r +
                ", z_r=" + z_r +
                ", id=" + id +
                '}';
    }
    // Getters and Setters
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public double getW_r() { return w_r; }
    public void setW_r(double w_r) { this.w_r = w_r; }

    public double getX_r() { return x_r; }
    public void setX_r(double x_r) { this.x_r = x_r; }

    public double getY_r() { return y_r; }
    public void setY_r(double y_r) { this.y_r = y_r; }

    public double getZ_r() { return z_r; }
    public void setZ_r(double z_r) { this.z_r = z_r; }

    public float[] getnextPos(){
        float x = (float) getX();
        float y = (float) getY();
        float z = (float) getZ();
        waypoint_pos[0] = x;
        waypoint_pos[1] = y;
        waypoint_pos[2] = z;
        return waypoint_pos;
    }

    public float[] getnextAngle(){
        float w_r = (float) getW_r();
        float x_r = (float) getX_r();
        float y_r = (float) getY_r();
        float z_r = (float) getZ_r();
        waypoint_angle[0] = w_r;
        waypoint_angle[1] = x_r;
        waypoint_angle[2] = y_r;
        waypoint_angle[3] = z_r;
        return waypoint_angle;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}