package com.example.demo.controller.diaryController;



import com.example.demo.model.diaryModel.DiaryEntry;
import com.example.demo.repository.DiaryEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/entries")
public class DiaryEntryController {

    @Autowired
    private DiaryEntryRepository repository;

    // GET all entries
    @GetMapping
    public List<DiaryEntry> getAllEntries() {
        return repository.findAllByOrderByEntryDateDesc();
    }

    // CREATE a new entry
    @PostMapping
    public DiaryEntry createEntry(@RequestBody DiaryEntry entry) {
        return repository.save(entry);
    }

    // UPDATE an entry
    @PutMapping("/{id}")
    public ResponseEntity<DiaryEntry> updateEntry(@PathVariable Long id, @RequestBody DiaryEntry entryDetails) {
        DiaryEntry entry = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found for this id :: " + id));

        entry.setTitle(entryDetails.getTitle());
        entry.setContent(entryDetails.getContent());
        // Do NOT allow changing the date on update

        final DiaryEntry updatedEntry = repository.save(entry);
        return ResponseEntity.ok(updatedEntry);
    }

    // DELETE an entry
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}