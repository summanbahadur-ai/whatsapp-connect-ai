-- Triggers attached to identity.users are deliberately outside the public-schema
-- dump and must be installed explicitly after the domain tables/functions exist.
CREATE TRIGGER on_identity_user_created AFTER INSERT ON identity.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
CREATE TRIGGER sync_identity_profile AFTER UPDATE OF name,email,image ON identity.users
  FOR EACH ROW EXECUTE FUNCTION public.sync_identity_profile();

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA identity FROM PUBLIC, wc_app, wc_worker;
REVOKE ALL ON ALL TABLES IN SCHEMA identity FROM PUBLIC, wc_app, wc_worker;
GRANT USAGE ON SCHEMA identity TO wc_auth;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA identity TO wc_auth;

GRANT USAGE ON SCHEMA public, app TO wc_app, wc_worker;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA app FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.current_user_id() TO wc_app, wc_worker;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wc_app, wc_worker;
DO $$ BEGIN
  IF to_regclass('public.flyway_schema_history') IS NOT NULL THEN
    REVOKE ALL ON public.flyway_schema_history FROM PUBLIC, wc_app, wc_worker, wc_auth;
  END IF;
END $$;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO wc_app, wc_worker;

-- Membership/identity projection is changed by reviewed functions/triggers only.
REVOKE INSERT, UPDATE, DELETE ON public.profiles FROM wc_app;
GRANT UPDATE(full_name, avatar_url) ON public.profiles TO wc_app;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM PUBLIC, wc_app, wc_auth;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO wc_worker;
GRANT EXECUTE ON FUNCTION public.is_account_member(uuid, public.account_role_enum) TO wc_app;
GRANT EXECUTE ON FUNCTION public.set_member_role(uuid, public.account_role_enum) TO wc_app;
GRANT EXECUTE ON FUNCTION public.remove_account_member(uuid) TO wc_app;
GRANT EXECUTE ON FUNCTION public.transfer_account_ownership(uuid) TO wc_app;
GRANT EXECUTE ON FUNCTION public.peek_invitation(text) TO wc_app;
GRANT EXECUTE ON FUNCTION public.redeem_invitation(text) TO wc_app;
GRANT EXECUTE ON FUNCTION public.filter_contacts_by_tags(uuid[], text, integer, integer) TO wc_app;
GRANT EXECUTE ON FUNCTION public.match_ai_knowledge_fts(uuid, text, integer) TO wc_app;
GRANT EXECUTE ON FUNCTION public.touch_presence(text) TO wc_app;
ALTER DEFAULT PRIVILEGES FOR ROLE wc_owner IN SCHEMA public REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE wc_owner IN SCHEMA identity REVOKE ALL ON TABLES FROM PUBLIC;
