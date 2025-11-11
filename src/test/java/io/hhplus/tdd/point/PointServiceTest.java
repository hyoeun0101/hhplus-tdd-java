package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    @InjectMocks
    PointService pointService;

    @Mock
    UserPointTable userPointTable;

    @Mock
    PointHistoryTable pointHistoryTable;

    @Test
    @DisplayName("존재하는 유저ID로 포인트 조회 성공")
    public void givenExistUserId_whenGetPoint_thenReturnUserPoint() {
        //given
        long validUserId = 1L;
        UserPoint expectedUserPoint = new UserPoint(validUserId, 1000L, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli());
        when(userPointTable.selectById(validUserId)).thenReturn(expectedUserPoint);

        //when
        UserPoint result = pointService.getUserPoint(validUserId);

        //then
        assertThat(result.id()).isEqualTo(validUserId);
        assertThat(result.point()).isEqualTo(expectedUserPoint.point());
        assertThat(result.updateMillis()).isEqualTo(expectedUserPoint.updateMillis());
    }

    @Test
    @DisplayName("존재하지 않은 유저ID로 포인트 조회 시 예외 발생")
    public void givenNotExistUserId_whenGetPoint_thenThrowException() {
        //given
        long invalidUserId = 0L;
        when(userPointTable.selectById(invalidUserId)).thenReturn(null);

        //when & then
        assertThrows(IllegalArgumentException.class, () -> pointService.getUserPoint(invalidUserId));
        verify(userPointTable, times(1)).selectById(invalidUserId);
    }


    @Test
    @DisplayName("유저ID로 포인트 내역 조회 성공")
    public void givenUserId_whenGetPointHistories_thenReturnPointHistoryList() {
        //given
        long validUserId = 1L;
        List<PointHistory> expected = List.of(
                new PointHistory(1L, validUserId, 1000L, TransactionType.CHARGE, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli())
                , new PointHistory(1L, validUserId, 500L, TransactionType.USE, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli())
        );
        when(pointHistoryTable.selectAllByUserId(validUserId)).thenReturn(expected);

        //when
        List<PointHistory> pointHistories = pointService.getPointHistories(validUserId);

        //then
        assertThat(pointHistories).isEqualTo(expected);
    }

    @Test
    @DisplayName("유저 ID로 포인트 내역 조회 시 내역이 없으면 빈 리스트 반환")
    public void givenUserId_whenGetPointHistories_thenReturnEmptyList() {
        //given
        long validUserId = 1L;
        when(pointHistoryTable.selectAllByUserId(validUserId)).thenReturn(List.of());

        //when
        List<PointHistory> pointHistories = pointService.getPointHistories(validUserId);

        //then
        assertThat(pointHistories).isEqualTo(List.of());
    }

    @Test
    @DisplayName("유효한 유저 ID로 포인트를 충전하면 사용자 포인트가 증가하고, 내역이 저장된다.")
    public void givenUserPoint_whenChargePoint_thenSuccess() {

        //given
        long userId = 1L;
        long chargePoint = 1000L;
        long now = Instant.parse("2025-11-10T00:00:00Z").toEpochMilli();
        UserPoint userPoint = new UserPoint(userId, 5000L, now);

        long updatedPoint = userPoint.point() + chargePoint;
        UserPoint updatedUserPoint = new UserPoint(userId, updatedPoint, now);

        PointHistory pointHistory = new PointHistory(1L, userId, chargePoint, TransactionType.CHARGE, now);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, updatedPoint)).thenReturn(updatedUserPoint);
        when(pointHistoryTable.insert(eq(userId), eq(chargePoint), eq(TransactionType.CHARGE), anyLong()))
                .thenReturn(pointHistory);

        //when
        UserPoint result = pointService.chargeUserPoint(userId, chargePoint);

        //then
        assertThat(result).isEqualTo(updatedUserPoint);
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, updatedPoint);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(chargePoint), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 포인트를 충전하면 예외가 발생한다.")
    public void givenNotExistUserId_whenChargePoint_thenThrowException() {
        //given
        long userId = 0L;
        long chargePoint = 1000L;
        when(userPointTable.selectById(userId)).thenReturn(null);

        //when & then
        assertThrows(IllegalArgumentException.class, () -> pointService.chargeUserPoint(userId, chargePoint));
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("충전포인트가 최소충전포인트 미만이면 예외가 발생한다.")
    public void givenInvalidChargePoint_whenChargePoint_thenThrowException() {
        //given
        long userId = 1L;
        long chargePoint = UserPoint.MIN_CHARGE_AMOUNT - 1L;
        long now = Instant.parse("2025-11-10T00:00:00Z").toEpochMilli();

        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000L, now));

        //when & then
        assertThatThrownBy(() -> pointService.chargeUserPoint(userId, chargePoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전포인트가 최소 충전포인트(10P) 미만입니다.");
    }

}