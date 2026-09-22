import { Clock, ChevronRight, User } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { NEEDS_ATTENTION_MOCK } from './mockData'

const STATUS_STYLE = {
  'no-action': { label: 'No Action Taken', dot: 'bg-[#ba0000]', text: 'text-[#ba0000]' },
  escalated: { label: 'Escalated', dot: 'bg-[#cf7e00]', text: 'text-[#cf7e00]' },
} as const

export function NeedsAttentionPanel() {
  return (
    <div className="rounded-lg border border-border bg-card">
      <div className="flex items-center justify-between border-b border-border px-5 py-4">
        <div className="flex items-center gap-2">
          <Clock className="size-4 text-foreground" />
          <h2 className="text-base font-semibold">Needs Attention</h2>
        </div>
        <a href="#" className="flex items-center gap-1 text-xs font-semibold text-brand hover:underline">
          Open agenda drill-down
          <ChevronRight className="size-3" />
        </a>
      </div>

      <div className="flex flex-col divide-y divide-border md:flex-row md:divide-x md:divide-y-0">
        {NEEDS_ATTENTION_MOCK.map((item) => {
          const status = STATUS_STYLE[item.status]
          return (
            <div key={item.id} className="flex flex-1 flex-col justify-between gap-4 p-4">
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-1">
                  <span className={cn('size-3 rounded-full', status.dot)} />
                  <span className={cn('text-xs font-semibold', status.text)}>{status.label}</span>
                </div>
                <p className="text-xs font-bold text-muted-foreground">
                  {item.category} · {item.date}
                </p>
                <p className="text-sm font-semibold">{item.kpiName}</p>
              </div>

              <div className="flex flex-col gap-2">
                <p className="text-xs text-muted-foreground">{item.detail}</p>
                <div className="flex items-center justify-between">
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <User className="size-3" />
                    {item.assignee}
                  </span>
                  {item.action === 'nudge' ? (
                    <Button size="xs" variant="outline" className="border-brand text-brand hover:bg-brand/10">
                      Nudge
                    </Button>
                  ) : (
                    <Button size="xs" className="bg-amber-500 text-white hover:bg-amber-600">
                      Act Now
                    </Button>
                  )}
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
