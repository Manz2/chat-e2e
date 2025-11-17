import './App.css';
import { appConfig } from './config/env.ts';

const apiModules = [
  {
    name: 'usersApi',
    responsibilities: [
      'Users listing & lookup',
      'Registration flow',
      'Public device discovery',
    ],
  },
  {
    name: 'deviceApi',
    responsibilities: [
      'Device enrollment lifecycle',
      'Inbox pull sync',
      'Owner-driven revocation',
    ],
  },
  {
    name: 'conversationsApi',
    responsibilities: [
      'Conversation provisioning',
      'Control key distribution',
      'Message fan-out',
    ],
  },
  {
    name: 'deliveriesApi',
    responsibilities: [
      'Delivery acknowledgements',
      'Read receipts over HTTP',
    ],
  },
  {
    name: 'syncApi',
    responsibilities: ['Bootstrap manifests for logged-in devices'],
  },
  {
    name: 'ChatRealtimeClient',
    responsibilities: [
      'STOMP/WebSocket transport',
      'Realtime message + read propagation',
    ],
  },
];

function App() {
  return (
    <main className="app">
      <header>
        <h1>Chat E2E Client Toolkit</h1>
        <p>
          API calls are routed to <code>{appConfig.backendUrl}</code>, Redis is
          configured via <code>{appConfig.redisUrl}</code>, and websocket events
          use <code>{appConfig.websocketUrl}</code>.
        </p>
      </header>

      <section className="module-grid">
        {apiModules.map((module) => (
          <article key={module.name}>
            <h2>{module.name}</h2>
            <ul>
              {module.responsibilities.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        ))}
      </section>
    </main>
  );
}

export default App;
