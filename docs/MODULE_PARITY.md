# Feature and endpoint parity inventory

Captured from the source working tree on 11 November 2026. These are requirements/reference facts, not implemented Java features.

Source: `D:\PyCharm-Projects\omni-connect-ai`; HEAD `193cab132c8c60cfe7d5e36985a6aa0360771c6a`; branch `feature-openapi-docs`.

Inventory: **90 API route files, 151 documented operations, 24 page files, 41 tables and 122 source unit/integration test files**.

The source OpenAPI includes common generated Better Auth operations; the catch-all may own additional routes. The source handler and tests take precedence over documentation. SQL inventory includes historical statements in the current migration set; inspect final constraints/functions/policies when porting.

## Module ownership and acceptance map

| Capability | Java package | Source implementation | Checkpoints |
| --- | --- | --- | --- |
| Auth/account/session/security | auth, security, account | src/lib/auth; src/lib/db/context*, account*, invitations* | CP02, CP04–06, CP09 |
| Existing React UI and Java transport | frontend | src/app; src/components; src/hooks; messages | CP03 and each module |
| Dashboard/presence/notifications | dashboard, notifications | src/lib/dashboard; src/lib/notifications; API routes | CP07, CP26 |
| Profile/settings/storage | settings, storage | settings components; src/lib/storage; stored_objects SQL | CP08–14 |
| Contacts/tags/custom values/notes/import | contacts | src/lib/contacts; src/lib/db/contacts* | CP13, CP15 |
| Inbox/messages/reactions/media | inbox, whatsapp, storage | src/lib/inbox; src/lib/media; src/lib/whatsapp; src/lib/db/inbound* | CP16–17 |
| Templates/quick replies | templates, quickreplies | src/lib/whatsapp/template*; src/lib/db/quick-replies* | CP11–12 |
| Pipelines/deals | pipelines | src/app/api/pipelines; src/app/api/deals; SQL repositories | CP13, CP18 |
| Broadcasts and recipient lifecycle | broadcasts | src/lib/whatsapp/broadcast*; src/lib/db/broadcasts* | CP19 |
| Visual flows, graph/runtime/events | flows | src/lib/flows; src/lib/db/flows* | CP20–21 |
| Automations, branches/waits/triggers | automations | src/lib/automations; src/lib/db/automations* | CP22–23 |
| AI/config/providers/knowledge/usage | ai | src/lib/ai; src/lib/db/ai-store* | CP24–25 |
| Public API/keys/outbound webhooks | publicapi, webhooks | src/app/api/v1; src/lib/api-keys; src/lib/webhooks | CP14, CP27 |
| OpenAPI and API Docs | apidocs + frontend UI | src/lib/openapi; API/docs page and client | CP28 |
| Optional MCP tools | optional Java adapter | mcp-server/src | CP29 |
| Scheduler/coordination/operations | jobs + scripts/infra | scripts/local-jobs.mjs; db worker-lock*; Docker files | CP17, CP19, CP21, CP23, CP30 |

## Source UI routes

| Page | Source file |
| --- | --- |
| `/forgot-password` | `src/app/(auth)/forgot-password/page.tsx` |
| `/login` | `src/app/(auth)/login/page.tsx` |
| `/reset-password` | `src/app/(auth)/reset-password/page.tsx` |
| `/signup` | `src/app/(auth)/signup/page.tsx` |
| `/agents` | `src/app/(dashboard)/agents/page.tsx` |
| `/api-docs` | `src/app/(dashboard)/api-docs/page.tsx` |
| `/automations/[id]/edit` | `src/app/(dashboard)/automations/[id]/edit/page.tsx` |
| `/automations/[id]/logs` | `src/app/(dashboard)/automations/[id]/logs/page.tsx` |
| `/automations/new` | `src/app/(dashboard)/automations/new/page.tsx` |
| `/automations` | `src/app/(dashboard)/automations/page.tsx` |
| `/broadcasts/[id]` | `src/app/(dashboard)/broadcasts/[id]/page.tsx` |
| `/broadcasts/new` | `src/app/(dashboard)/broadcasts/new/page.tsx` |
| `/broadcasts` | `src/app/(dashboard)/broadcasts/page.tsx` |
| `/contacts` | `src/app/(dashboard)/contacts/page.tsx` |
| `/dashboard` | `src/app/(dashboard)/dashboard/page.tsx` |
| `/flows/[id]` | `src/app/(dashboard)/flows/[id]/page.tsx` |
| `/flows/[id]/runs` | `src/app/(dashboard)/flows/[id]/runs/page.tsx` |
| `/flows` | `src/app/(dashboard)/flows/page.tsx` |
| `/inbox` | `src/app/(dashboard)/inbox/page.tsx` |
| `/notifications` | `src/app/(dashboard)/notifications/page.tsx` |
| `/pipelines` | `src/app/(dashboard)/pipelines/page.tsx` |
| `/settings` | `src/app/(dashboard)/settings/page.tsx` |
| `/join/[token]` | `src/app/join/[token]/page.tsx` |
| `/` | `src/app/page.tsx` |

## Settings sections

Overview, Profile, Security, Appearance, WhatsApp, Templates, Quick Replies, Fields/Tags, Deals, Members and API. Preserve URL-based section selection and legacy aliases.

## Flow and automation type parity

- Flow nodes (10): start, send_message, send_buttons, send_list, send_media, collect_input, condition, set_tag, handoff, end.
- Flow triggers: keyword, first_inbound_message, manual. Preserve keyword semantics and reserved/ignored validation behavior.
- Automation triggers (8): new_message_received, first_inbound_message, keyword_match, new_contact_created, conversation_assigned, tag_added, time_based, interactive_reply.
- Automation steps (13): send_message, send_buttons, send_list, send_template, add_tag, remove_tag, assign_conversation, update_contact_field, create_deal, wait, condition, send_webhook, close_conversation.
- Port actual behavior from engine/validation tests; comments about supported schedules or future nodes do not establish implemented behavior.

## API operation inventory

Mark each operation in a target parity ledger as pending/implemented/tested/user-accepted. Associate a Java controller test and owning checkpoint before marking tested. The JSON contract includes the corresponding schemas/parameters and authored examples.

| Method | Path | Module/tag | Scope or role | Behavior |
| --- | --- | --- | --- | --- |
| GET | `/api/auth-context` | Account | viewer | Read current profile and account |
| GET | `/api/openapi` | Account | viewer | Download the OpenAPI specification |
| GET | `/api/account` | Account | viewer | Read account settings and role |
| PATCH | `/api/account` | Account | admin | Rename the current account |
| PATCH | `/api/account/currency` | Account | admin | Set default currency |
| GET | `/api/account/members` | Account | viewer | List account members |
| PATCH | `/api/account/members/{userId}` | Account | admin | Change a member role |
| DELETE | `/api/account/members/{userId}` | Account | admin | Remove a member |
| POST | `/api/account/transfer-ownership` | Account | owner | Transfer account ownership |
| GET | `/api/account/invitations` | Account | admin | List invitations |
| POST | `/api/account/invitations` | Account | admin | Create an invitation |
| DELETE | `/api/account/invitations/{id}` | Account | admin | Revoke an invitation |
| GET | `/api/invitations/{token}/peek` | Account | [] | Inspect an invitation token |
| POST | `/api/invitations/{token}/redeem` | Account | viewer | Join the invited account |
| GET | `/api/account/api-keys` | API keys | viewer | List API key metadata |
| POST | `/api/account/api-keys` | API keys | admin | Create a scoped API key |
| DELETE | `/api/account/api-keys/{id}` | API keys | admin | Revoke an API key |
| GET | `/api/contacts` | Contacts | viewer | List/search contacts |
| POST | `/api/contacts` | Contacts | agent | Create a contact |
| DELETE | `/api/contacts` | Contacts | agent | Delete selected contacts |
| GET | `/api/contacts/{id}` | Contacts | viewer | Read a contact |
| PATCH | `/api/contacts/{id}` | Contacts | agent | Update a contact |
| GET | `/api/contacts/duplicate` | Contacts | viewer | Find an existing phone match |
| POST | `/api/contacts/import` | Contacts | agent | Import a validated contact batch |
| GET | `/api/contacts/{id}/tags` | Contacts | viewer | List a contact’s tag links |
| POST | `/api/contacts/{id}/tags` | Contacts | agent | Add a tag to a contact |
| DELETE | `/api/contacts/{id}/tags` | Contacts | agent | Remove a tag from a contact |
| GET | `/api/contacts/{id}/notes` | Contacts | viewer | List contact notes |
| POST | `/api/contacts/{id}/notes` | Contacts | agent | Add a contact note |
| DELETE | `/api/contacts/{id}/notes` | Contacts | agent | Delete a contact note |
| GET | `/api/contacts/{id}/custom-values` | Contacts | viewer | Read custom field values |
| PUT | `/api/contacts/{id}/custom-values` | Contacts | agent | Set custom field values |
| GET | `/api/contacts/{id}/deals` | Contacts | viewer | List a contact’s deals |
| GET | `/api/dashboard` | Dashboard | viewer | Read dashboard metrics or activity |
| GET | `/api/pipelines` | Pipelines and deals | viewer | List pipelines |
| POST | `/api/pipelines` | Pipelines and deals | admin | Create a pipeline |
| GET | `/api/pipelines/{id}` | Pipelines and deals | viewer | List pipeline stages |
| PATCH | `/api/pipelines/{id}` | Pipelines and deals | admin | Save pipeline name and stages |
| POST | `/api/pipelines/{id}` | Pipelines and deals | admin | Add a pipeline stage |
| DELETE | `/api/pipelines/{id}` | Pipelines and deals | admin | Delete a stage or the whole pipeline |
| GET | `/api/deals` | Pipelines and deals | viewer | List deals in a pipeline |
| POST | `/api/deals` | Pipelines and deals | agent | Create a deal |
| PATCH | `/api/deals/{id}` | Pipelines and deals | agent | Update a deal |
| DELETE | `/api/deals/{id}` | Pipelines and deals | agent | Delete a deal |
| GET | `/api/deals/options` | Pipelines and deals | viewer | Read deal-editor resources |
| GET | `/api/inbox/conversations` | Inbox | viewer | List account conversations |
| GET | `/api/inbox/conversations/{id}` | Inbox | viewer | Read conversation details |
| PATCH | `/api/inbox/conversations/{id}` | Inbox | agent | Update conversation state |
| GET | `/api/inbox/conversations/{id}/messages` | Inbox | viewer | Read a conversation’s messages and reactions |
| GET | `/api/inbox/poll` | Inbox | viewer | Poll inbox snapshots |
| GET | `/api/notifications` | Notifications and presence | viewer | Read own notifications |
| PATCH | `/api/notifications` | Notifications and presence | viewer | Mark notifications as read |
| GET | `/api/presence` | Notifications and presence | viewer | Read member presence |
| POST | `/api/presence` | Notifications and presence | viewer | Update own presence |
| GET | `/api/unread-counts` | Notifications and presence | viewer | Read own unread totals |
| GET | `/api/settings/overview` | Settings | viewer | Read account configuration overview |
| GET | `/api/settings/tags` | Settings | viewer | List available tags |
| POST | `/api/settings/tags` | Settings | admin | Create a tag |
| DELETE | `/api/settings/tags` | Settings | admin | Delete a tag |
| GET | `/api/settings/custom-fields` | Settings | viewer | List custom field definitions |
| POST | `/api/settings/custom-fields` | Settings | admin | Create a custom field |
| PATCH | `/api/settings/custom-fields` | Settings | admin | Rename a custom field |
| DELETE | `/api/settings/custom-fields` | Settings | admin | Delete a custom field |
| GET | `/api/settings/templates` | Templates | viewer | Read the local template catalogue |
| GET | `/api/quick-replies` | Quick replies | viewer | List saved quick replies |
| POST | `/api/quick-replies` | Quick replies | agent | Create a quick reply |
| PATCH | `/api/quick-replies/{id}` | Quick replies | agent | Update a quick reply |
| DELETE | `/api/quick-replies/{id}` | Quick replies | agent | Delete a quick reply |
| GET | `/api/flows` | Flows | viewer | List conversation flows |
| POST | `/api/flows` | Flows | agent | Create a flow or copy a gallery template |
| GET | `/api/flows/templates` | Flows | viewer | Read flow template gallery |
| GET | `/api/flows/{id}` | Flows | viewer | Read a flow and its graph |
| PUT | `/api/flows/{id}` | Flows | agent | Save flow configuration and graph |
| DELETE | `/api/flows/{id}` | Flows | agent | Delete a flow |
| POST | `/api/flows/{id}/activate` | Flows | agent | Validate and change flow status |
| GET | `/api/flows/{id}/runs` | Flows | viewer | Read flow run history |
| GET | `/api/automations` | Automations | viewer | List event automations |
| POST | `/api/automations` | Automations | agent | Create an automation |
| GET | `/api/automations/{id}` | Automations | viewer | Read automation configuration and steps |
| PATCH | `/api/automations/{id}` | Automations | agent | Save automation configuration and steps |
| DELETE | `/api/automations/{id}` | Automations | agent | Delete an automation |
| POST | `/api/automations/{id}/duplicate` | Automations | agent | Duplicate an automation |
| GET | `/api/automations/{id}/logs` | Automations | viewer | Read automation execution logs |
| GET | `/api/automations/resources` | Automations | viewer | Read automation-builder resources |
| POST | `/api/automations/engine` | Automations | agent | Dispatch an automation trigger |
| GET | `/api/broadcasts` | Broadcasts | viewer | List campaigns |
| POST | `/api/broadcasts` | Broadcasts | agent | Create a draft or start a broadcast |
| GET | `/api/broadcasts/{id}` | Broadcasts | viewer | Read campaign and recipient results |
| DELETE | `/api/broadcasts/{id}` | Broadcasts | agent | Delete a campaign |
| POST | `/api/broadcasts/audience` | Broadcasts | viewer | Preview audience size |
| POST | `/api/whatsapp/broadcast/{id}/resume` | Broadcasts | agent | Resume pending or failed recipients |
| POST | `/api/whatsapp/broadcast` | Broadcasts | agent | Send an immediate template batch |
| GET | `/api/whatsapp/config` | WhatsApp connection | viewer | Test saved WhatsApp credentials |
| POST | `/api/whatsapp/config` | WhatsApp connection | admin | Validate and save WhatsApp connection |
| DELETE | `/api/whatsapp/config` | WhatsApp connection | admin | Reset local WhatsApp configuration |
| GET | `/api/whatsapp/config/details` | WhatsApp connection | viewer | Read safe WhatsApp settings metadata |
| PATCH | `/api/whatsapp/config/details` | WhatsApp connection | admin | Set incoming-media mirroring |
| GET | `/api/whatsapp/config/verify-registration` | WhatsApp connection | viewer | Probe Meta registration readiness |
| POST | `/api/whatsapp/send` | WhatsApp messages | agent | Send a dashboard inbox message |
| POST | `/api/whatsapp/react` | WhatsApp messages | agent | Send or clear an agent reaction |
| GET | `/api/whatsapp/media/{mediaId}` | WhatsApp messages | viewer | Proxy referenced incoming media |
| POST | `/api/whatsapp/templates/sync` | Templates | admin | Sync templates from Meta |
| POST | `/api/whatsapp/templates/submit` | Templates | admin | Submit a template to Meta |
| PATCH | `/api/whatsapp/templates/{id}` | Templates | admin | Update a local or Meta template |
| DELETE | `/api/whatsapp/templates/{id}` | Templates | admin | Delete a template locally and on Meta where applicable |
| GET | `/api/ai/config` | AI assistant | viewer | Read redacted AI configuration |
| POST | `/api/ai/config` | AI assistant | admin | Validate and save AI configuration |
| DELETE | `/api/ai/config` | AI assistant | admin | Delete AI configuration |
| POST | `/api/ai/test` | AI assistant | admin | Test AI provider credentials |
| POST | `/api/ai/draft` | AI assistant | agent | Generate a reply draft |
| POST | `/api/ai/playground` | AI assistant | agent | Test the assistant in a playground |
| POST | `/api/ai/autoreply/{conversationId}` | AI assistant | agent | Pause or resume conversation auto-reply |
| GET | `/api/ai/usage` | AI assistant | viewer | Read AI usage totals |
| GET | `/api/ai/knowledge` | Knowledge | viewer | List knowledge document metadata |
| POST | `/api/ai/knowledge` | Knowledge | admin | Create and index a knowledge document |
| GET | `/api/ai/knowledge/{id}` | Knowledge | viewer | Read a knowledge document |
| PATCH | `/api/ai/knowledge/{id}` | Knowledge | admin | Update and reindex a knowledge document |
| DELETE | `/api/ai/knowledge/{id}` | Knowledge | admin | Delete a knowledge document |
| POST | `/api/ai/knowledge/reindex` | Knowledge | admin | Reindex account knowledge |
| POST | `/api/uploads/{bucket}` | Uploads | viewer | Upload raw file bytes |
| DELETE | `/api/uploads/{bucket}` | Uploads | viewer | Delete an owned upload |
| GET | `/api/automations/cron` | Integrations (reference only) | [{"CronSecret":[]}] | Drain due automation waits |
| GET | `/api/flows/cron` | Integrations (reference only) | [{"CronSecret":[]}] | Sweep expired flow runs |
| GET | `/api/whatsapp/webhook` | Integrations (reference only) | [] | Verify the Meta webhook callback |
| POST | `/api/whatsapp/webhook` | Integrations (reference only) | [{"MetaSignature":[]}] | Receive signed Meta events |
| GET | `/api/v1/me` | Public API — account | [{"ApiKey":[]}] | Check API-key account and capabilities |
| GET | `/api/v1/contacts` | Public API — contacts | contacts:read | List contacts using cursor pagination |
| POST | `/api/v1/contacts` | Public API — contacts | contacts:write | Find/create a contact and replace supplied tags |
| GET | `/api/v1/contacts/{id}` | Public API — contacts | contacts:read | Read a contact |
| PATCH | `/api/v1/contacts/{id}` | Public API — contacts | contacts:write | Update a contact or its tags |
| GET | `/api/v1/conversations` | Public API — inbox | conversations:read | List conversations |
| GET | `/api/v1/conversations/{id}` | Public API — inbox | conversations:read | Read a conversation |
| GET | `/api/v1/conversations/{id}/messages` | Public API — inbox | messages:read | List conversation messages |
| POST | `/api/v1/messages` | Public API — messages | messages:send | Send a WhatsApp message to a customer |
| POST | `/api/v1/broadcasts` | Public API — broadcasts | broadcasts:send | Create and start a broadcast |
| GET | `/api/v1/broadcasts/{id}` | Public API — broadcasts | broadcasts:send | Read broadcast status |
| GET | `/api/v1/webhooks` | Public API — webhooks | webhooks:manage | List outbound event destinations |
| POST | `/api/v1/webhooks` | Public API — webhooks | webhooks:manage | Register an outbound event destination |
| GET | `/api/v1/webhooks/{id}` | Public API — webhooks | webhooks:manage | Read an event destination |
| PATCH | `/api/v1/webhooks/{id}` | Public API — webhooks | webhooks:manage | Update an event destination |
| DELETE | `/api/v1/webhooks/{id}` | Public API — webhooks | webhooks:manage | Delete an event destination |
| GET | `/api/auth/get-session` | Authentication | [] | Read the current Better Auth session |
| POST | `/api/auth/sign-up/email` | Authentication | [] | Sign up with email and password |
| POST | `/api/auth/sign-in/email` | Authentication | [] | Sign in with email and password |
| POST | `/api/auth/sign-out` | Authentication | viewer | Sign out the current browser |
| POST | `/api/auth/request-password-reset` | Authentication | [] | Request a password-reset email |
| POST | `/api/auth/reset-password` | Authentication | [] | Set a password using a reset token |
| POST | `/api/auth/update-user` | Authentication | viewer | Update own name or avatar |
| POST | `/api/auth/change-email` | Authentication | viewer | Request a verified email change |
| GET | `/api/auth/list-sessions` | Authentication | viewer | List own active sessions |
| POST | `/api/auth/revoke-session` | Authentication | viewer | Revoke one of your sessions |

## Explicit source route files

| Source handler | Methods |
| --- | --- |
| `src/app/api/account/api-keys/[id]/route.ts` | DELETE |
| `src/app/api/account/api-keys/route.ts` | GET, POST |
| `src/app/api/account/currency/route.ts` | PATCH |
| `src/app/api/account/invitations/[id]/route.ts` | DELETE |
| `src/app/api/account/invitations/route.ts` | GET, POST |
| `src/app/api/account/members/[userId]/route.ts` | PATCH, DELETE |
| `src/app/api/account/members/route.ts` | GET |
| `src/app/api/account/route.ts` | GET, PATCH |
| `src/app/api/account/transfer-ownership/route.ts` | POST |
| `src/app/api/ai/autoreply/[conversationId]/route.ts` | POST |
| `src/app/api/ai/config/route.ts` | GET, DELETE, POST |
| `src/app/api/ai/draft/route.ts` | POST |
| `src/app/api/ai/knowledge/[id]/route.ts` | GET, DELETE, PATCH |
| `src/app/api/ai/knowledge/reindex/route.ts` | POST |
| `src/app/api/ai/knowledge/route.ts` | GET, POST |
| `src/app/api/ai/playground/route.ts` | POST |
| `src/app/api/ai/test/route.ts` | POST |
| `src/app/api/ai/usage/route.ts` | GET |
| `src/app/api/auth-context/route.ts` | GET |
| `src/app/api/auth/[...all]/route.ts` | GET, POST |
| `src/app/api/automations/[id]/duplicate/route.ts` | POST |
| `src/app/api/automations/[id]/logs/route.ts` | GET |
| `src/app/api/automations/[id]/route.ts` | GET, PATCH, DELETE |
| `src/app/api/automations/cron/route.ts` | GET |
| `src/app/api/automations/engine/route.ts` | POST |
| `src/app/api/automations/resources/route.ts` | GET |
| `src/app/api/automations/route.ts` | GET, POST |
| `src/app/api/broadcasts/[id]/route.ts` | GET, DELETE |
| `src/app/api/broadcasts/audience/route.ts` | POST |
| `src/app/api/broadcasts/route.ts` | GET, POST |
| `src/app/api/contacts/[id]/custom-values/route.ts` | GET, PUT |
| `src/app/api/contacts/[id]/deals/route.ts` | GET |
| `src/app/api/contacts/[id]/notes/route.ts` | GET, POST, DELETE |
| `src/app/api/contacts/[id]/route.ts` | GET, PATCH |
| `src/app/api/contacts/[id]/tags/route.ts` | GET, POST, DELETE |
| `src/app/api/contacts/duplicate/route.ts` | GET |
| `src/app/api/contacts/import/route.ts` | POST |
| `src/app/api/contacts/route.ts` | GET, POST, DELETE |
| `src/app/api/dashboard/route.ts` | GET |
| `src/app/api/deals/[id]/route.ts` | PATCH, DELETE |
| `src/app/api/deals/options/route.ts` | GET |
| `src/app/api/deals/route.ts` | GET, POST |
| `src/app/api/flows/[id]/activate/route.ts` | POST |
| `src/app/api/flows/[id]/route.ts` | GET, PUT, DELETE |
| `src/app/api/flows/[id]/runs/route.ts` | GET |
| `src/app/api/flows/cron/route.ts` | GET |
| `src/app/api/flows/route.ts` | GET, POST |
| `src/app/api/flows/templates/route.ts` | GET |
| `src/app/api/inbox/conversations/[id]/messages/route.ts` | GET |
| `src/app/api/inbox/conversations/[id]/route.ts` | GET, PATCH |
| `src/app/api/inbox/conversations/route.ts` | GET |
| `src/app/api/inbox/poll/route.ts` | GET |
| `src/app/api/invitations/[token]/peek/route.ts` | GET |
| `src/app/api/invitations/[token]/redeem/route.ts` | POST |
| `src/app/api/notifications/route.ts` | GET, PATCH |
| `src/app/api/openapi/route.ts` | GET |
| `src/app/api/pipelines/[id]/route.ts` | GET, PATCH, POST, DELETE |
| `src/app/api/pipelines/route.ts` | GET, POST |
| `src/app/api/presence/route.ts` | GET, POST |
| `src/app/api/quick-replies/[id]/route.ts` | PATCH, DELETE |
| `src/app/api/quick-replies/route.ts` | GET, POST |
| `src/app/api/settings/custom-fields/route.ts` | GET, POST, PATCH, DELETE |
| `src/app/api/settings/overview/route.ts` | GET |
| `src/app/api/settings/tags/route.ts` | GET, POST, DELETE |
| `src/app/api/settings/templates/route.ts` | GET |
| `src/app/api/unread-counts/route.ts` | GET |
| `src/app/api/uploads/[bucket]/route.ts` | POST, DELETE |
| `src/app/api/v1/broadcasts/[id]/route.ts` | GET |
| `src/app/api/v1/broadcasts/route.ts` | POST |
| `src/app/api/v1/contacts/[id]/route.ts` | GET, PATCH |
| `src/app/api/v1/contacts/route.ts` | GET, POST |
| `src/app/api/v1/conversations/[id]/messages/route.ts` | GET |
| `src/app/api/v1/conversations/[id]/route.ts` | GET |
| `src/app/api/v1/conversations/route.ts` | GET |
| `src/app/api/v1/me/route.ts` | GET |
| `src/app/api/v1/messages/route.ts` | POST |
| `src/app/api/v1/webhooks/[id]/route.ts` | GET, PATCH, DELETE |
| `src/app/api/v1/webhooks/route.ts` | GET, POST |
| `src/app/api/whatsapp/broadcast/[id]/resume/route.ts` | POST |
| `src/app/api/whatsapp/broadcast/route.ts` | POST |
| `src/app/api/whatsapp/config/details/route.ts` | GET, PATCH |
| `src/app/api/whatsapp/config/route.ts` | GET, POST, DELETE |
| `src/app/api/whatsapp/config/verify-registration/route.ts` | GET |
| `src/app/api/whatsapp/media/[mediaId]/route.ts` | GET |
| `src/app/api/whatsapp/react/route.ts` | POST |
| `src/app/api/whatsapp/send/route.ts` | POST |
| `src/app/api/whatsapp/templates/[id]/route.ts` | PATCH, DELETE |
| `src/app/api/whatsapp/templates/submit/route.ts` | POST |
| `src/app/api/whatsapp/templates/sync/route.ts` | POST |
| `src/app/api/whatsapp/webhook/route.ts` | GET, POST |

## Source database table inventory

Domain tables are candidates to preserve. Better Auth identity/session internals need Java-owned replacements; never copy all identity DDL blindly.

- `identity.credentials`
- `identity.sessions`
- `identity.users`
- `identity.verifications`
- `public.account_invitations`
- `public.accounts`
- `public.ai_configs`
- `public.ai_knowledge_chunks`
- `public.ai_knowledge_documents`
- `public.ai_usage_log`
- `public.api_keys`
- `public.automation_logs`
- `public.automation_pending_executions`
- `public.automation_steps`
- `public.automations`
- `public.broadcast_recipients`
- `public.broadcasts`
- `public.contact_custom_values`
- `public.contact_notes`
- `public.contact_tags`
- `public.contacts`
- `public.conversations`
- `public.custom_fields`
- `public.deals`
- `public.flow_nodes`
- `public.flow_run_events`
- `public.flow_runs`
- `public.flows`
- `public.member_presence`
- `public.message_reactions`
- `public.message_templates`
- `public.messages`
- `public.notifications`
- `public.pipeline_stages`
- `public.pipelines`
- `public.profiles`
- `public.quick_replies`
- `public.stored_objects`
- `public.tags`
- `public.webhook_endpoints`
- `public.whatsapp_config`

## Other parity assets

- Existing source unit/integration tests: paths and hashes in SOURCE_BASELINE.json. Port behavior assertions rather than matching a test count.
- Browser test journeys: current tests/browser and per-checkpoint scenarios; add Java-auth/CSRF and real PostgreSQL isolation checks.
- Languages: en, ko, pt, es. Preserve existing UI strings and mode/mobile behavior.
- API-key scopes: messages:send, messages:read, contacts:read, contacts:write, conversations:read, broadcasts:send, webhooks:manage.
- Existing public API/MCP documentation and live-provider prerequisites remain behavioral reference, not evidence of a tested Java adapter.

## Completion ledger template

| Source operation/feature | Target controller/service/UI | CP | Automated evidence | User review | Difference/reason |
| --- | --- | --- | --- | --- | --- |
| Populate one row per source operation and additional non-HTTP feature before final parity sign-off | Pending | Pending | Not run | Pending | None approved |

Optional modules need an explicit enabled/tested or user-approved disabled entry. Non-migrated modules remain unavailable; never fake their data or call the old app.
