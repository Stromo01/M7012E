package com.dji.sdk.sample.demo.flightcontroller;

public class Waypoints {
    private float x;
    private float y;
    private float z;
    private float w_r;
    private float x_r;
    private float y_r;
    private float z_r;

    private float angle;
    private int id;

    private float[] waypoint_pos = new float[3];  // Initialize array
    private float[] waypoint_angle = new float[4]; // Initialize array
    // Default Constructor
    public Waypoints() {}

    // Parameterized Constructor
    public Waypoints(float x, float y, float z, float w_r, float x_r, float y_r, float z_r, float angle,int id) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w_r = w_r;
        this.x_r = x_r;
        this.y_r = y_r;
        this.z_r = z_r;
        this.angle=angle;
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
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getZ() { return z; }
    public void setZ(float z) { this.z = z; }

    public float getW_r() { return w_r; }
    public void setW_r(float w_r) { this.w_r = w_r; }

    public float getX_r() { return x_r; }
    public void setX_r(float x_r) { this.x_r = x_r; }

    public float getY_r() { return y_r; }
    public void setY_r(float y_r) { this.y_r = y_r; }

    public float getZ_r() { return z_r; }
    public void setZ_r(float z_r) { this.z_r = z_r; }

    public float[] getnextPos(){
        float x = this.x;
        float y =  this.y;
        float z =  this.z;
        waypoint_pos[0] = x;
        waypoint_pos[1] = y;
        waypoint_pos[2] = z;
        return waypoint_pos;
    }

    public float getnextAngle(){
        float w_r = getW_r();
        float x_r =  getX_r();
        float y_r = getY_r();
        float z_r = getZ_r();
        waypoint_angle[0] = w_r;
        waypoint_angle[1] = x_r;
        waypoint_angle[2] = y_r;
        waypoint_angle[3] = z_r;
        return angle;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}