# UML Diagrams

All diagrams are written as code with PlantUML; PNG exports are in the `png/` folder.
Class and method names in the diagrams match `CLAUDE.md` and `docs/TASKS.md`; when the code changes, the diagrams are updated too (C7, Y7).

| File | Type | Content | Author |
|---|---|---|---|
| `use-case.puml` | Use Case | Actors (Visitor, Pet Owner, Veterinarian, Scheduler) and UC-01…UC-23 | Ceylin (C1) |
| `otp-login-sequence.puml` | Sequence | Code request and code verification flow, error cases | Ceylin (C1) |
| `adoption-activity.puml` | Activity | From application to ownership transfer (Visitor / System / Veterinarian swimlanes) | Ceylin (C1) |
| `er.puml` | ER | Database tables and relationships | Yusuf (Y1) |
| `class-domain.puml` | Class | Entities and enums | Yusuf (Y1) |
| `class-notification.puml` | Class | Notification layer (Strategy + Factory) | Yusuf (Y1) |
| `class-adoption-state.puml` | Class | Listing states (State) | Yusuf (Y1) |
| `adoption-state.puml` | State | Listing lifecycle | Yusuf (Y1) |
| `deployment.puml` | Deployment | Docker Compose: app, db, mailpit | Yusuf (Y1) |

Use case numbers match the requirements (FR/NFR) in `docs/requirements.md`.

## Exporting PNGs

**IntelliJ:** install the *PlantUML Integration* (plantuml4idea) plugin → open a `.puml` file → click inside the code → in the preview panel use **Save diagram** → `docs/uml/png/<file-name>.png`.

**Command line:**
```bash
java -jar plantuml.jar -tpng -o png docs/uml/*.puml
```

Use case and activity diagrams require **Graphviz**. If `dot` is not on the PATH, set the path in IntelliJ under Settings → PlantUML → Graphviz dot executable (e.g. `C:\Program Files\Graphviz\bin\dot.exe`), or add `!pragma layout smetana` below the `@startuml` line. Sequence diagrams do not need Graphviz.
