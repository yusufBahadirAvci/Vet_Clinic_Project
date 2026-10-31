package com.vetclinic.pet.service;

import com.vetclinic.common.AgeFormatter;
import com.vetclinic.common.PhotoUrls;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.vaccination.VaccinationStatus;
import com.vetclinic.vaccination.VaccinationStatusCalculator;
import com.vetclinic.vaccination.domain.Vaccination;
import com.vetclinic.vaccination.repository.VaccinationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Facade for the owner portal: combines a pet, its vaccinations and the status calculation
 * into DTOs so controllers and views never touch entities.
 * <p>
 * Status rule: for each vaccine only the latest record counts; older doses count as completed.
 */
@Service
@Transactional(readOnly = true)
public class PetProfileFacade {

    private static final Comparator<Vaccination> NEWEST_FIRST =
            Comparator.comparing(Vaccination::getAdministeredAt).thenComparing(Vaccination::getId).reversed();

    private final PetRepository petRepository;
    private final VaccinationRepository vaccinationRepository;
    private final VaccinationStatusCalculator statusCalculator;
    private final AgeFormatter ageFormatter;

    public PetProfileFacade(PetRepository petRepository, VaccinationRepository vaccinationRepository,
                            VaccinationStatusCalculator statusCalculator, AgeFormatter ageFormatter) {
        this.petRepository = petRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.statusCalculator = statusCalculator;
        this.ageFormatter = ageFormatter;
    }

    /** The owner's pets for the card list, ordered by name. */
    public List<PetSummaryDto> listForOwner(Long ownerId) {
        return petRepository.findAllByOwnerIdOrderByName(ownerId).stream()
                .map(this::toSummary)
                .toList();
    }

    /** @throws NotFoundException if the pet does not exist or belongs to someone else */
    public PetProfileDto profileOf(Long petId, Long ownerId) {
        Pet pet = petRepository.findByIdAndOwnerId(petId, ownerId)
                .orElseThrow(() -> new NotFoundException("Pet not found: " + petId));
        List<PetProfileDto.VaccinationEntry> timeline = timelineOf(pet);
        List<PetProfileDto.VaccinationEntry> upcoming = timeline.stream()
                .filter(PetProfileFacade::needsAttention)
                .sorted(Comparator.comparing(PetProfileDto.VaccinationEntry::nextDueDate))
                .toList();
        return new PetProfileDto(
                pet.getId(), pet.getName(), pet.getSpecies(), label(pet.getSpecies()), pet.getBreed(),
                label(pet.getGender()), pet.getBirthDate(), ageFormatter.format(pet.getBirthDate()),
                pet.getColor(), pet.getMicrochipNo(), pet.isNeutered(), PhotoUrls.safeOrNull(pet.getPhotoUrl()),
                nearestStatus(timeline), upcoming, timeline);
    }

    private PetSummaryDto toSummary(Pet pet) {
        return new PetSummaryDto(pet.getId(), pet.getName(), pet.getSpecies(), label(pet.getSpecies()),
                pet.getBreed(), ageFormatter.format(pet.getBirthDate()), PhotoUrls.safeOrNull(pet.getPhotoUrl()),
                nearestStatus(timelineOf(pet)));
    }

    /** All records, newest to oldest; only the first record of each vaccine is marked current. */
    private List<PetProfileDto.VaccinationEntry> timelineOf(Pet pet) {
        List<Vaccination> records = new ArrayList<>(
                vaccinationRepository.findAllByPetIdOrderByAdministeredAtDesc(pet.getId()));
        records.sort(NEWEST_FIRST);
        Set<Long> seenVaccines = new HashSet<>();
        List<PetProfileDto.VaccinationEntry> entries = new ArrayList<>();
        for (Vaccination v : records) {
            boolean current = seenVaccines.add(v.getVaccine().getId());
            VaccinationStatus status = current ? statusCalculator.statusOf(v.getNextDueDate()) : VaccinationStatus.NONE;
            entries.add(new PetProfileDto.VaccinationEntry(v.getVaccine().getName(), v.getAdministeredAt(),
                    v.getNextDueDate(), v.getVet().getFullName(), v.getLotNumber(), status, current));
        }
        return entries;
    }

    /** Status of the earliest next-due date among the current records; NONE if no dose is scheduled. */
    private VaccinationStatus nearestStatus(List<PetProfileDto.VaccinationEntry> timeline) {
        return timeline.stream()
                .filter(PetProfileDto.VaccinationEntry::current)
                .filter(e -> e.nextDueDate() != null)
                .map(PetProfileDto.VaccinationEntry::nextDueDate)
                .min(Comparator.naturalOrder())
                .map(statusCalculator::statusOf)
                .orElse(VaccinationStatus.NONE);
    }

    private static boolean needsAttention(PetProfileDto.VaccinationEntry entry) {
        return entry.current()
                && (entry.status() == VaccinationStatus.OVERDUE || entry.status() == VaccinationStatus.DUE_SOON);
    }

    private static String label(Enum<?> value) {
        String name = value.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
