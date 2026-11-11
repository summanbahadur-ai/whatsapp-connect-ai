import type { ComponentType, createElement } from 'react';

interface Definition {
  has: (name: string) => boolean;
}
interface Definitions {
  filter: (predicate: (definition: Definition) => boolean) => Definitions;
}
interface OperationData {
  get: (name: string) => unknown;
  set: (name: string, value: unknown) => OperationData;
}
interface OperationProps extends Record<string, unknown> {
  operation: OperationData;
}

/** Swagger's public selector hooks keep manual authorization limited to API keys. */
export function docsBrowserPlugin() {
  return {
    wrapComponents: {
      // Preserve the container's state mapping; wrap its presentation component.
      operation: (
        Original: ComponentType<OperationProps>,
        system: { React: { createElement: typeof createElement } }
      ) =>
        function ReferenceOnlyOperation(props: OperationProps) {
          const blocked = [
            '/api/automations/cron',
            '/api/flows/cron',
            '/api/whatsapp/webhook',
          ].includes(String(props.operation.get('path')));
          // Use Swagger's bundled React, which can differ from the app's React.
          return system.React.createElement(Original, {
            ...props,
            operation: blocked
              ? props.operation.set('allowTryItOut', false)
              : props.operation,
          });
        },
    },
    statePlugins: {
      auth: {
        wrapSelectors: {
          definitionsToAuthorize: (original: () => Definitions) => () =>
            original().filter((definition) => definition.has('ApiKey')),
        },
      },
    },
  };
}
