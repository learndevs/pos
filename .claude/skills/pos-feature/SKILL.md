---
name: pos-feature
description: The end-to-end workflow for adding or changing a POS feature (spec -> DB -> domain -> API -> UI -> buttons/devices -> docs -> verify), with code skeletons and a definition of done. Use at the start of any new feature or behaviour change, before writing code.
---

# Adding a feature

Read first: `docs/architecture.md` (system map, request flow, sale lifecycle, where code goes).
Area skills: `pos-domain` (rules), `pos-backend`, `pos-sale-actions` (till buttons), `pos-desktop` (UI, themes, devices), `pos-printing`, `pos-docs`.

## 0. Spec (5 minutes, always)
Copy `docs/features/_template.md` to `docs/features/<name>.md`; fill Why, Flow, Rules, Data, API, UI, Permissions.
Add a row to `docs/features/README.md`. If you chose between options, add a `D-0nn` row in `docs/decisions.md`.
Ask the user only when a choice is genuinely theirs (money rules, who may do it, what the customer sees).

## 1. Database: `server/app/src/main/resources/db/migration/V<next>__<desc>.sql`
- Next number = highest existing + 1. Never edit an applied migration.
- uuid PKs (app-assigned), money `bigint` minor units, enums as `text` + `CHECK`, timestamps `timestamptz`.
- Seed reference rows here (permissions, `pos_function`, `menu_button`, settings).
- Dev demo data only in `db/demo/`.

## 2. Domain (server module `server/modules/<feature>`, package `com.retailpos.server.<feature>`)
New area? Create the module (see `pos-backend` layout). Need another module's behaviour without a dependency cycle? Publish/handle an event (example `SaleCompleted`).
```java
@Entity @Table(name = "thing")
public class Thing extends BaseEntity {                 // UUIDv7 id, Persistable
    @Column(nullable = false) private String name;
    protected Thing() {}
    public static Thing create(String name) { Thing t = new Thing(true); t.name = name; return t; }
    private Thing(boolean newRow) { super(newRow); }
    public void rename(String name) {                    // rules live here
        if (name.isBlank()) throw PosException.badRequest("Name is required.");
        this.name = name;
    }
}
public interface ThingRepository extends JpaRepository<Thing, UUID> {}
```
Changes to sale lines go through `PosTransaction` methods only.

## 3. Service + API
```java
@Service @Transactional
public class ThingService {
    private final ThingRepository things; private final Clock clock;
    ThingService(ThingRepository things, Clock clock) { this.things = things; this.clock = clock; }
    public ThingView create(String name) { return ThingView.of(things.save(Thing.create(name))); }
}

@RestController @RequestMapping("/api/things")
public class ThingController {
    public record CreateThing(@NotBlank @Size(max = 80) String name) {}
    private final ThingService things;
    ThingController(ThingService things) { this.things = things; }
    @PostMapping @PreAuthorize("hasAuthority('THING_MANAGE')")
    public ThingView create(@Valid @RequestBody CreateThing req) { return things.create(req.name()); }
}
```
- Return view records, never entities. New permission? Seed it in the migration (`role_permission`).
- Test (light): one `@PosIntegrationTest @Transactional` flow test when money, stock or permissions change.

## 4. UI
- API calls: `ui/src/features/<feature>/<feature>Api.ts` using `api` / `post` / `apiText` from `api/client.ts`.
- Screens: `ui/src/features/<feature>/*.tsx`; style with CSS tokens (`var(--surface)` etc.) so every theme works; check Default and Liquid Glass.
- Server state: TanStack Query; screen state: Zustand store in the feature folder.

## 5. Till buttons (if the sale screen gets a new behaviour)
Follow `pos-sale-actions` checklist: `pos_function` row + `menu_button` placement (4 rows x 3 cols) in the migration, then `defineAction({ id, needs, input?, confirm?, run })` returning effects.

## 6. Devices (if hardware is involved)
`desktop/src/devices/<device>.ts` + IPC channel in `main.ts` + method in `preload.ts` + type in `ui/src/types/pos-bridge.d.ts` + wrapper in `ui/src/devices/devices.ts` (with a browser fallback). Provide a **test window** version first.

## 7. Verify (definition of done)
1. `cd server && ./gradlew test` (if Gradle is killed with exit 137, rerun with `-Dorg.gradle.jvmargs=-Xmx1g`).
2. `cd ui && npx tsc -b --noEmit && npx vite build`; `cd desktop && npx tsc -p tsconfig.json --noEmit`.
3. Run it: `tools/e2e/test-stack.sh up`, drive the flow (copy `tools/e2e/sale-cycle.mjs`), look at the screenshots in `tools/e2e/out/`, then `tools/e2e/test-stack.sh down`.
4. Update the feature spec, `docs/architecture.md` if flows changed, the area skill if a convention changed, README tables if files were added.
5. Report what was verified and what was not.
