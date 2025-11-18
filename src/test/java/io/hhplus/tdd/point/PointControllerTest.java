package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PointController.class)
class PointControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    PointService pointService;
    @Autowired
    ObjectMapper objectMapper;


    @Test
    @DisplayName("포인트 조회 성공")
    void getUserPoint_success() throws Exception {
        //given
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 1000L, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli());
        when(pointService.getUserPoint(userId)).thenReturn(userPoint);

        //when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(userPoint.point()));
    }


    @Test
    @DisplayName("포인트 내역 조회 성공")
    void getPointHistories_success() throws Exception {
        //given
        long userId = 5L;
        List<PointHistory> pointHistories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli())

                , new PointHistory(2L, userId, 500L, TransactionType.USE, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli())
        );
        when(pointService.getPointHistories(userId)).thenReturn(pointHistories);

        //when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(pointHistories.get(0).amount()))
                .andExpect(jsonPath("$[0].type").value(pointHistories.get(0).type().name()))
                .andExpect(jsonPath("$[1].userId").value(userId))
                .andExpect(jsonPath("$[1].amount").value(pointHistories.get(1).amount()))
                .andExpect(jsonPath("$[1].type").value(pointHistories.get(1).type().name()));
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint_success() throws Exception {
        //given
        long userId = 1L;
        long amount = 1000L;
        UserPoint updatedUserPoint = new UserPoint(userId, 500L + amount, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli());
        when(pointService.chargeUserPoint(userId, amount)).thenReturn(updatedUserPoint);

        //when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(1500L));
    }

    @Test
    @DisplayName("충전할 포인트가 최소 충전포인트 미만일 경우 400 status 응답 ")
    void chargePoint_fail() throws Exception {
        //given
        long userId = 1L;
        long amount = 1L;
        when(pointService.chargeUserPoint(userId, amount)).thenThrow(new IllegalStateException("충전포인트가 최소 충전포인트(" + UserPoint.MIN_CHARGE_AMOUNT + "P) 미만입니다."));

        //when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount))
                ).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_success() throws Exception {
        //given
        long userId = 1L;
        long amount = 700L;
        UserPoint updatedUserPoint = new UserPoint(userId, 1000L - amount, Instant.parse("2025-11-10T00:00:00Z").toEpochMilli());

        when(pointService.usePoint(userId, amount)).thenReturn(updatedUserPoint);

        //when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount))
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(300L));
    }

    @Test
    @DisplayName("포인트 사용 시 잔액이 부족한 경우 400 status 반환")
    void usePoint_fail() throws Exception {
        //given
        long userId = 1L;
        long amount = 700L;

        when(pointService.usePoint(userId, amount)).thenThrow(new IllegalStateException("잔액이 부족합니다."));

        //when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount))
                ).andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}