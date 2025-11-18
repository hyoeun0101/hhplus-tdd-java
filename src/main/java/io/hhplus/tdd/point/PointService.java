package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final UserPointTable userPointTable;

    private final PointHistoryTable pointHistoryTable;

    public UserPoint getUserPoint(long userId) {

        UserPoint userPoint = userPointTable.selectById(userId);

        if (userPoint == null) {
            throw new IllegalArgumentException("userId [" + userId + "] not found");
        }
        return userPoint;
    }

    public List<PointHistory> getPointHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint chargeUserPoint(long userId, long chargePoint) {
        UserPoint userPoint = userPointTable.selectById(userId);
        if (userPoint == null) {
            throw new IllegalArgumentException("userId [" + userId + "] not found");
        }
        userPoint.validateChargePoint(chargePoint);

        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, userPoint.point() + chargePoint);

        pointHistoryTable.insert(userId, chargePoint, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedUserPoint;
    }

    public UserPoint usePoint(long userId, long usePoint) {

        UserPoint userPoint = userPointTable.selectById(userId);
        if (userPoint == null) {
            throw new IllegalArgumentException("userId [" + userId + "] not found");
        }
        userPoint.validateUsePoint(usePoint);

        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, userPoint.point() - usePoint);

        pointHistoryTable.insert(userId, usePoint, TransactionType.USE, System.currentTimeMillis());
        return updatedUserPoint;
    }
}
