package br.com.belloinfo.saap_mvp.domain.model;

import br.com.belloinfo.saap_mvp.domain.valueobject.ProfessionalRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Professional {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String registrationNumber;
    private ProfessionalRole role;
    private boolean active;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
