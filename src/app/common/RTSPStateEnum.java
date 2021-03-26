package app.common;

public enum RTSPStateEnum {
    INIT(0),
    READY(1),
    PLAYING(2),
    ;

    int numericId;

    RTSPStateEnum(int numericId){
        this.numericId = numericId;
    }

    public int getNumericId(){
        return this.numericId;
    }
}
