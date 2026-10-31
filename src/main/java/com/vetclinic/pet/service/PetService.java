package com.vetclinic.pet.service;

import com.vetclinic.common.AgeFormatter;
import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.common.web.EnumLabels;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.pet.repository.PetSpecifications;
import com.vetclinic.pet.web.OwnerOption;
import com.vetclinic.pet.web.PetDetails;
import com.vetclinic.pet.web.PetFilter;
import com.vetclinic.pet.web.PetForm;
import com.vetclinic.pet.web.PetListItem;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Pet management in the vet panel. All business rules for pets live here. */
@Service
public class PetService {

    public static final int PAGE_SIZE = 20;

    static final String BIRTH_DATE_IN_FUTURE = "The birth date cannot be in the future.";
    static final String MICROCHIP_TAKEN = "This microchip number is used by another pet.";
    static final String UNKNOWN_OWNER = "Please select an existing owner.";
    static final String INACTIVE_OWNER = "This owner is inactive. Reactivate the owner first.";

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final AgeFormatter ageFormatter;
    private final Clock clock;

    public PetService(PetRepository petRepository, UserRepository userRepository, AgeFormatter ageFormatter,
                      Clock clock) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.ageFormatter = ageFormatter;
        this.clock = clock;
    }

    /** Pets matching the filter, 20 per page, sorted by name. */
    @Transactional(readOnly = true)
    public Page<PetListItem> search(PetFilter filter) {
        Specification<Pet> spec = Specification.unrestricted();
        if (filter.getSpecies() != null) {
            spec = spec.and(PetSpecifications.hasSpecies(filter.getSpecies()));
        }
        if (filter.getOwnership() == PetFilter.Ownership.OWNED) {
            spec = spec.and(PetSpecifications.hasOwner());
        } else if (filter.getOwnership() == PetFilter.Ownership.OWNERLESS) {
            spec = spec.and(PetSpecifications.hasNoOwner());
        }
        if (filter.getQ() != null && !filter.getQ().isBlank()) {
            spec = spec.and(PetSpecifications.nameOrMicrochipContains(filter.getQ()));
        }
        PageRequest pageRequest = PageRequest.of(Math.max(filter.getPage(), 0), PAGE_SIZE, Sort.by("name", "id"));
        return petRepository.findAll(spec, pageRequest).map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public PetDetails getDetails(Long id) {
        Pet pet = findPet(id);
        User owner = pet.getOwner();
        return new PetDetails(pet.getId(), pet.getName(), EnumLabels.of(pet.getSpecies()), pet.getBreed(),
                EnumLabels.of(pet.getGender()), pet.getBirthDate(), ageFormatter.format(pet.getBirthDate()),
                pet.getColor(), pet.getMicrochipNo(), pet.isNeutered(), pet.getPhotoUrl(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getFullName(),
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getPhone());
    }

    /** Empty form; the owner is preselected when {@code ownerId} is an active owner, otherwise ignored. */
    @Transactional(readOnly = true)
    public PetForm newForm(Long ownerId) {
        PetForm form = new PetForm();
        if (ownerId != null) {
            userRepository.findById(ownerId)
                    .filter(u -> u.getRole() == Role.OWNER && u.isActive())
                    .ifPresent(u -> form.setOwnerId(u.getId()));
        }
        return form;
    }

    @Transactional(readOnly = true)
    public PetForm getForm(Long id) {
        Pet pet = findPet(id);
        PetForm form = new PetForm();
        form.setName(pet.getName());
        form.setSpecies(pet.getSpecies());
        form.setBreed(pet.getBreed());
        form.setGender(pet.getGender());
        form.setBirthDate(pet.getBirthDate());
        form.setColor(pet.getColor());
        form.setMicrochipNo(pet.getMicrochipNo());
        form.setNeutered(pet.isNeutered());
        form.setPhotoUrl(pet.getPhotoUrl());
        form.setOwnerId(pet.getOwner() == null ? null : pet.getOwner().getId());
        return form;
    }

    /**
     * Active owners for the select box, sorted by name. {@code selectedOwnerId} is kept even if that owner is
     * inactive, so editing a pet of an inactive owner does not silently drop the owner.
     */
    @Transactional(readOnly = true)
    public List<OwnerOption> ownerOptions(Long selectedOwnerId) {
        return userRepository.findAllByRoleOrderByFullName(Role.OWNER).stream()
                .filter(u -> u.isActive() || u.getId().equals(selectedOwnerId))
                .map(u -> new OwnerOption(u.getId(), optionLabel(u)))
                .toList();
    }

    /** Creates a pet; returns the new id. */
    @Transactional
    public Long create(PetForm form) {
        Pet pet = new Pet();
        apply(form, pet, null);
        return petRepository.save(pet).getId();
    }

    @Transactional
    public void update(Long id, PetForm form) {
        Pet pet = findPet(id);
        apply(form, pet, id);
    }

    private Pet findPet(Long id) {
        return petRepository.findById(id).orElseThrow(() -> new NotFoundException("Pet not found: " + id));
    }

    /** Validates and copies the form onto the entity. {@code ownId} is the pet being edited (null when creating). */
    private void apply(PetForm form, Pet pet, Long ownId) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (form.getBirthDate() != null && form.getBirthDate().isAfter(LocalDate.now(clock))) {
            errors.put("birthDate", BIRTH_DATE_IN_FUTURE);
        }

        String microchip = blankToNull(form.getMicrochipNo());
        if (microchip != null) {
            microchip = microchip.toUpperCase(Locale.ROOT);
            boolean taken = ownId == null ? petRepository.existsByMicrochipNo(microchip)
                    : petRepository.existsByMicrochipNoAndIdNot(microchip, ownId);
            if (taken) {
                errors.put("microchipNo", MICROCHIP_TAKEN);
            }
        }

        User owner = null;
        if (form.getOwnerId() != null) {
            owner = userRepository.findById(form.getOwnerId()).filter(u -> u.getRole() == Role.OWNER).orElse(null);
            Long currentOwnerId = pet.getOwner() == null ? null : pet.getOwner().getId();
            if (owner == null) {
                errors.put("ownerId", UNKNOWN_OWNER);
            } else if (!owner.isActive() && !Objects.equals(owner.getId(), currentOwnerId)) {
                errors.put("ownerId", INACTIVE_OWNER);
            }
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }

        pet.setName(form.getName().trim());
        pet.setSpecies(form.getSpecies());
        pet.setBreed(blankToNull(form.getBreed()));
        pet.setGender(form.getGender());
        pet.setBirthDate(form.getBirthDate());
        pet.setColor(blankToNull(form.getColor()));
        pet.setMicrochipNo(microchip);
        pet.setNeutered(form.isNeutered());
        pet.setPhotoUrl(blankToNull(form.getPhotoUrl()));
        pet.setOwner(owner);
    }

    private PetListItem toListItem(Pet pet) {
        User owner = pet.getOwner();
        return new PetListItem(pet.getId(), pet.getName(), EnumLabels.of(pet.getSpecies()), pet.getBreed(),
                ageFormatter.format(pet.getBirthDate()), pet.getMicrochipNo(),
                owner == null ? null : owner.getId(), owner == null ? null : owner.getFullName());
    }

    private static String optionLabel(User owner) {
        String contact = owner.getEmail() != null ? owner.getEmail() : owner.getPhone();
        String label = owner.getFullName() + " (" + contact + ")";
        return owner.isActive() ? label : label + " – inactive";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
