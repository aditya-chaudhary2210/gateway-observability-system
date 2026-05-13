import React from 'react';

type IncidentRow = {
  incidentId: string;
  openedAt: string;
  severity: string;
  status: string;
  primaryClassification?: string | null;
  fingerprint?: string | null;
};

function useIncidents(limit: number) {
  const [data, setData] = React.useState<Array<IncidentRow>>([]);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let aborted = false;
    async function load() {
      try {
        const response = await fetch(`/api/v1/incidents?limit=${limit}`, { credentials: 'same-origin' });
        if (!response.ok) {
          throw new Error(await response.text());
        }
        const payload = await response.json();
        if (!aborted) {
          setData(payload as Array<IncidentRow>);
        }
      } catch (cause) {
        if (!aborted) {
          setError(cause instanceof Error ? cause.message : 'unknown-error');
        }
      }
    }
    void load();
    const handle = window.setInterval(() => void load(), 10000);
    return () => {
      aborted = true;
      window.clearInterval(handle);
    };
  }, [limit]);

  return { incidents: data, error };
}

export default function App() {
  const { incidents, error } = useIncidents(40);

  return (
    <div className="min-h-screen bg-obs-canvas text-slate-100">
      <header className="border-b border-slate-700 bg-gradient-to-r from-slate-900 via-slate-900 to-blue-950">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-6 py-12">
          <p className="text-sm uppercase tracking-widest text-slate-400">Gateway Observability</p>
          <h1 className="text-3xl font-semibold text-white">Incident intelligence cockpit</h1>
          <p className="text-slate-300">
            Streams `/api/v1/incidents/**` proxied via the SPA&apos;s nginx sidecar toward Spring Cloud Gateway, mirroring docs/
            SERVICE_BOUNDARIES.
          </p>
        </div>
      </header>

      <main className="mx-auto flex max-w-6xl flex-col gap-8 px-6 py-10">
        {error !== null ? (
          <div className="rounded-md border border-rose-500/40 bg-rose-950/40 p-6 text-sm text-rose-200">
            Incident feed unavailable ({error}). Ensure `docker compose` stack is healthy.
          </div>
        ) : null}

        <section className="rounded-xl border border-slate-700/60 bg-slate-900/60 p-6 shadow-xl shadow-blue-950/40 backdrop-blur">
          <header className="mb-6 flex flex-col gap-1">
            <h2 className="text-xl font-medium text-white">Incident feed</h2>
            <p className="text-sm text-slate-400">Auto-refreshing every 10s</p>
          </header>

          <div className="overflow-auto">
            <table className="w-full divide-y divide-slate-700 text-sm">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wide text-slate-400">
                  <th className="pb-4 pr-4">Opened</th>
                  <th className="pb-4 pr-4">Severity</th>
                  <th className="pb-4 pr-4">Status</th>
                  <th className="pb-4 pr-4">Classification</th>
                  <th className="pb-4">Fingerprint</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {incidents.length === 0 && error === null ? (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-slate-500">
                      Awaiting ingestion + detection emits — generate traffic via `/mock/hello` behind the gateway.
                    </td>
                  </tr>
                ) : null}
                {incidents.map(row => (
                  <tr key={row.incidentId} className="align-top hover:bg-white/5">
                    <td className="py-4 pr-4 font-mono text-xs text-slate-300">{row.openedAt}</td>
                    <td className="py-4 pr-4">
                      <span className="rounded-full bg-orange-900/70 px-2 py-1 text-[11px] font-semibold text-orange-200">
                        {row.severity}
                      </span>
                    </td>
                    <td className="py-4 pr-4 text-slate-200">{row.status}</td>
                    <td className="py-4 pr-4 text-slate-300">{row.primaryClassification ?? 'n/a'}</td>
                    <td className="py-4 font-mono text-[11px] text-emerald-400">{row.fingerprint}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}
