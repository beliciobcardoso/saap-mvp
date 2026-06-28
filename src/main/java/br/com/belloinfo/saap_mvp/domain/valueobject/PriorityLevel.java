package br.com.belloinfo.saap_mvp.domain.valueobject;

public enum PriorityLevel {
    P1(1),
    P2(2),
    P3(3),
    P4(4),
    P5(5);

    private final int value;

    PriorityLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
