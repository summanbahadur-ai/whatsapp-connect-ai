// Copied UI baseline for rules newly enabled by Next 16.4.
// Keep visible as warnings. Remove each entry when its module is migrated/tested.
// New transport/gating code is not covered by these overrides.
export default [
  {
    files: ["src/app/(dashboard)/automations/page.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/app/(dashboard)/broadcasts/page.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/app/(dashboard)/inbox/page.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/broadcasts/step2-select-audience.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/contacts/contact-detail-view.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/contacts/contact-form.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
      "react-hooks/immutability": "warn",
    },
  },
  {
    files: ["src/components/flows/flow-canvas.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/inbox/ai-thread-banner.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/inbox/message-composer.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/inbox/message-thread.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
      "react-hooks/preserve-manual-memoization": "warn",
    },
  },
  {
    files: ["src/components/inbox/quick-reply-picker.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/api-keys-settings.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/deals-settings.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/members-tab.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/profile-form.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/quick-replies-manager.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/tag-manager.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/template-manager.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/settings/whatsapp-config.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
  {
    files: ["src/components/tremor/bar-chart.tsx"],
    rules: {
      "react-hooks/set-state-in-effect": "warn",
    },
  },
];
