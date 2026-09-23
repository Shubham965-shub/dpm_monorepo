import { Search } from 'lucide-react'
import { Button } from '@/components/ui/button'

/** Filters the real KPI sections below (see SectionSummaryList) by label as you type - the button
 * doesn't do anything extra beyond preventing a page reload, since filtering is already live. */
export function SearchKpiBar({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <form
      onSubmit={(e) => e.preventDefault()}
      className="flex items-center gap-3 rounded-lg border border-border bg-card p-3"
    >
      <div className="flex flex-1 items-center gap-2 rounded-md border border-input bg-background px-3 py-2">
        <Search className="size-4 text-muted-foreground" />
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="Search KPI"
          className="w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
      </div>
      <Button type="submit" className="bg-brand hover:bg-brand-dark">
        Search
      </Button>
    </form>
  )
}
