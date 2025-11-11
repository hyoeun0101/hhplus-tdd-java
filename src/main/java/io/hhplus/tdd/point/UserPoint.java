package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static final long MIN_CHARGE_AMOUNT = 10L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public void validateChargePoint(long amount) {
        if (amount < MIN_CHARGE_AMOUNT) {
            throw new IllegalStateException("충전포인트가 최소 충전포인트(" + MIN_CHARGE_AMOUNT + "P) 미만입니다.");
        }
    }
}
