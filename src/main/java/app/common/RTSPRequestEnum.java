package app.common;

public enum RTSPRequestEnum {
    SETUP(3),
    PLAY(4),
    PAUSE(5),
    TEARDOWN(6),
    DESCRIBE(7),
    ;

    int numericId;

    RTSPRequestEnum(int numericId){
        this.numericId = numericId;
    }

    public static RTSPRequestEnum getEnum(int id) {
        for(RTSPRequestEnum anEnum : values()) {
            if(anEnum.getNumericId() == id)
                return anEnum;
        }
        return null;
    }

    public int getNumericId(){
        return this.numericId;
    }
}
