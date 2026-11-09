package fu.dlms.fine.controller;

import fu.dlms.fine.exception.GlobalExceptionHandler;
import fu.dlms.fine.service.FineService;
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

class MeFineControllerTest {

    private final FineService fines = mock(FineService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new MeFineController(fines)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsOnlyTheFinesOfTheMemberInTheHeader() throws Exception {
        when(fines.findByMember(42L)).thenReturn(List.of());

        mvc.perform(get("/api/me/fines").header("X-Member-Id", "42")).andExpect(status().isOk()).andExpect(content().json("[]"));

        verify(fines).findByMember(42L);
    }

    @Test
    void withoutTheHeaderTheCallerIsNotSignedIn() throws Exception {
        mvc.perform(get("/api/me/fines")).andExpect(status().isUnauthorized());
    }

    @Test
    void aMemberIdQueryParameterIsIgnored() throws Exception {
        when(fines.findByMember(42L)).thenReturn(List.of());

        mvc.perform(get("/api/me/fines?memberId=7").header("X-Member-Id", "42")).andExpect(status().isOk());

        verify(fines).findByMember(42L);
    }
}
