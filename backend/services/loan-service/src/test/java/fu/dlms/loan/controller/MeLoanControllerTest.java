package fu.dlms.loan.controller;

import fu.dlms.loan.exception.GlobalExceptionHandler;
import fu.dlms.loan.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeLoanControllerTest {

    private final LoanService loans = mock(LoanService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new MeLoanController(loans)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsOnlyTheLoansOfTheMemberInTheHeader() throws Exception {
        when(loans.findAll(null, 42L)).thenReturn(List.of());

        mvc.perform(get("/api/me/loans").header("X-Member-Id", "42")).andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(loans).findAll(null, 42L);
    }

    @Test
    void withoutTheHeaderTheCallerIsNotSignedIn() throws Exception {
        mvc.perform(get("/api/me/loans")).andExpect(status().isUnauthorized());
    }

    @Test
    void aMemberIdQueryParameterIsIgnored() throws Exception {
        when(loans.findAll(null, 42L)).thenReturn(List.of());

        mvc.perform(get("/api/me/loans?memberId=7").header("X-Member-Id", "42")).andExpect(status().isOk());

        verify(loans).findAll(null, 42L);
    }
}
