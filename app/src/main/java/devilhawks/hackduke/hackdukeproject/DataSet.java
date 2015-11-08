package devilhawks.hackduke.hackdukeproject;

class Dataset {
    String formattedDate;
    String formattedDuration;
    public float blinkRate;
    public float turnRate;
    public float tiltRate;
    public float pauseRate;
    public float pauseAvg;

    Dataset(String date,String duration, float blinkRate, float turnRate, float tiltRate, float pauseRate, float pauseAvg){
        this.formattedDate = date;
        this.blinkRate = blinkRate;
        this.turnRate = turnRate;
        this.tiltRate = tiltRate;
        this.pauseRate = pauseRate;
        this.pauseAvg = pauseAvg;
        this.formattedDuration = duration;
    }
}